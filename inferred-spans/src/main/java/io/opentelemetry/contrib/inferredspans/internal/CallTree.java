/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal;

import static io.opentelemetry.contrib.inferredspans.internal.semconv.Attributes.CODE_STACKTRACE;
import static io.opentelemetry.contrib.inferredspans.internal.semconv.Attributes.LINK_IS_CHILD;
import static io.opentelemetry.contrib.inferredspans.internal.semconv.Attributes.SPAN_IS_INFERRED;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.inferredspans.internal.pooling.ObjectPool;
import io.opentelemetry.contrib.inferredspans.internal.pooling.Recyclable;
import io.opentelemetry.contrib.inferredspans.internal.util.HexUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.agrona.collections.LongHashSet;

/**
 * Converts a sequence of stack traces into a tree structure of method calls.
 *
 * <pre>
 *             count
 *  b b     a      4
 * aaaa ──► ├─b    1
 *          └─b    1
 * </pre>
 *
 * <p>It also stores information about which span is the parent of a particular call tree node,
 * based on which span has been active at that time.
 *
 * <p>This allows to infer spans from the call tree which have the correct parent/child
 * relationships with the regular spans.
 */
@SuppressWarnings("javadoc")
public class CallTree implements Recyclable {

  private static final int INITIAL_CHILD_SIZE = 2;

  public static final Attributes CHILD_LINK_ATTRIBUTES =
      Attributes.builder().put(LINK_IS_CHILD, true).build();

  public static final BiConsumer<SpanBuilder, SpanContext> DEFAULT_PARENT_OVERRIDE =
      (inferredSpan, child) -> inferredSpan.addLink(child, CHILD_LINK_ATTRIBUTES);

  @Nullable private CallTree parent;
  protected int count;
  private List<CallTree> children = new ArrayList<>(INITIAL_CHILD_SIZE);
  @Nullable private StackFrame frame;
  protected long start;
  private long lastSeen;
  private boolean ended;
  private long activationTimestamp = -1;

  /**
   * The context of the transaction or span which is the direct parent of this call tree node. Used
   * in {@link #spanify} to override the parent.
   */
  @Nullable private TraceContext activeContextOfDirectParent;

  private long deactivationTimestamp = -1;
  private boolean isSpan;
  private int depth;

  @Nullable private ChildList childIds;

  @Nullable private ChildList maybeChildIds;

  public CallTree() {}

  public void set(@Nullable CallTree parent, StackFrame frame, long nanoTime) {
    this.parent = parent;
    this.frame = frame;
    this.start = nanoTime;
    if (parent != null) {
      this.depth = parent.depth + 1;
    }
  }

  public boolean isSuccessor(CallTree parent) {
    if (depth > parent.depth) {
      return getNthParent(depth - parent.depth) == parent;
    }
    return false;
  }

  @Nullable
  public CallTree getNthParent(int n) {
    CallTree parent = this;
    for (int i = 0; i < n; i++) {
      if (parent != null) {
        parent = parent.parent;
      } else {
        return null;
      }
    }
    return parent;
  }

  public void activation(@Nullable TraceContext traceContext, long activationTimestamp) {
    this.activeContextOfDirectParent = traceContext;
    this.activationTimestamp = activationTimestamp;
  }

  protected void handleDeactivation(
      TraceContext deactivatedSpan, long activationTimestamp, long deactivationTimestamp) {
    if (deactivatedSpan.idEquals(activeContextOfDirectParent)) {
      this.deactivationTimestamp = deactivationTimestamp;
    } else {
      CallTree lastChild = getLastChild();
      if (lastChild != null) {
        lastChild.handleDeactivation(deactivatedSpan, activationTimestamp, deactivationTimestamp);
      }
    }
    // if an actual child span is deactivated after this call tree node has ended
    // it means that this node has actually ended at least at the same point, if not after, the
    // actual span has been deactivated
    //
    // [a(inferred)]    ─► [a(inferred)  ] ← set end timestamp to timestamp of deactivation of b
    // └─[b(actual)  ]     └─[b(actual)  ]
    // see also CallTreeTest::testDectivationAfterEnd
    if (happenedDuring(activationTimestamp) && happenedAfter(deactivationTimestamp)) {
      lastSeen = deactivationTimestamp;
    }
  }

  private boolean happenedDuring(long timestamp) {
    return start <= timestamp && timestamp <= lastSeen;
  }

  private boolean happenedAfter(long timestamp) {
    return lastSeen < timestamp;
  }

  public static CallTree.Root createRoot(
      ObjectPool<Root> rootPool, byte[] traceContext, long nanoTime) {
    CallTree.Root root = rootPool.createInstance();
    root.set(traceContext, nanoTime);
    return root;
  }

  /**
   * Adds a single stack trace to the call tree which either updates the {@link #lastSeen} timestamp
   * of an existing call tree node, {@linkplain #end ends} a node, or {@linkplain #addChild adds a
   * new child}.
   *
   * @param stackFrames the stack trace which is iterated over in reverse order
   * @param index the current index of {@code stackFrames}
   * @param activeSpan the trace context of the currently active span
   * @param activationTimestamp the timestamp of when {@code traceContext} has been activated
   * @param nanoTime the timestamp of when this stack trace has been recorded
   */
  protected CallTree addFrame(
      List<StackFrame> stackFrames,
      int index,
      @Nullable TraceContext activeSpan,
      long activationTimestamp,
      long nanoTime,
      ObjectPool<CallTree> callTreePool,
      long minDurationNs,
      Root root) {
    count++;
    lastSeen = nanoTime;
    //     c ee   ← traceContext not set - they are not a child of the active span but the frame
    // below them
    //   bbb dd   ← traceContext set
    //   ------   ← all new CallTree during this period should have the traceContext set
    // a aaaaaa a
    //  |      |
    // active  deactive

    // this branch is already aware of the activation
    // this means the provided activeSpan is not a direct parent of new child nodes
    if (activeSpan != null
        && this.activeContextOfDirectParent != null
        && this.activeContextOfDirectParent.idEquals(activeSpan)) {
      activeSpan = null;
    }

    // non-last children are already ended by definition
    CallTree lastChild = getLastChild();
    // if the frame corresponding to the last child is not in the stack trace
    // it's assumed to have ended one tick ago
    CallTree topOfStack = this;
    boolean endChild = true;
    if (index >= 1) {
      StackFrame frame = stackFrames.get(--index);
      if (lastChild != null) {
        if (!lastChild.isEnded() && frame.equals(lastChild.frame)) {
          topOfStack =
              lastChild.addFrame(
                  stackFrames,
                  index,
                  activeSpan,
                  activationTimestamp,
                  nanoTime,
                  callTreePool,
                  minDurationNs,
                  root);
          endChild = false;
        } else {
          topOfStack =
              addChild(
                  frame,
                  stackFrames,
                  index,
                  activeSpan,
                  activationTimestamp,
                  nanoTime,
                  callTreePool,
                  minDurationNs,
                  root);
        }
      } else {
        topOfStack =
            addChild(
                frame,
                stackFrames,
                index,
                activeSpan,
                activationTimestamp,
                nanoTime,
                callTreePool,
                minDurationNs,
                root);
      }
    }
    if (lastChild != null && !lastChild.isEnded() && endChild) {
      lastChild.end(callTreePool, minDurationNs, root);
    }
    transferMaybeChildIdsToChildIds();
    return topOfStack;
  }

  /**
   * This method is called when we know for sure that the maybe child ids are actually belonging to
   * this call tree. This is the case after we've seen another frame represented by this call tree.
   *
   * @see #addMaybeChildId(long, long)
   */
  private void transferMaybeChildIdsToChildIds() {
    if (maybeChildIds != null) {
      if (childIds == null) {
        childIds = maybeChildIds;
        maybeChildIds = null;
      } else {
        childIds.addAll(maybeChildIds);
        maybeChildIds.clear();
      }
    }
  }

  private CallTree addChild(
      StackFrame frame,
      List<StackFrame> stackFrames,
      int index,
      @Nullable TraceContext traceContext,
      long activationTimestamp,
      long nanoTime,
      ObjectPool<CallTree> callTreePool,
      long minDurationNs,
      Root root) {
    CallTree callTree = callTreePool.createInstance();
    callTree.set(this, frame, nanoTime);
    if (traceContext != null) {
      callTree.activation(traceContext, activationTimestamp);
    }
    children.add(callTree);
    return callTree.addFrame(
        stackFrames, index, null, activationTimestamp, nanoTime, callTreePool, minDurationNs, root);
  }

  long getDurationUs() {
    return getDurationNs() / 1000;
  }

  private long getDurationNs() {
    return lastSeen - start;
  }

  public int getCount() {
    return count;
  }

  @Nullable
  public StackFrame getFrame() {
    return frame;
  }

  public List<CallTree> getChildren() {
    return children;
  }

  protected void end(ObjectPool<CallTree> pool, long minDurationNs, Root root) {
    ended = true;
    // if the parent span has already been deactivated before this call tree node has ended
    // it means that this node is actually the parent of the already deactivated span
    //                     make b parent of a and pre-date the start of b to the activation of a
    // [a(inferred)   ]     [a(inferred)   ]
    //  [1        ]     ──┐  [b(inferred) ]
    //  └[b(inferred)]    │  [c(inferred)]
    //   [c(infer.) ]     └► [1        ]
    //   └─[d(i.)]           └──[d(i.)]
    // see also CallTreeTest::testDeactivationBeforeEnd
    if (deactivationHappenedBeforeEnd()) {
      start = Math.min(activationTimestamp, start);
      if (parent != null) {
        // we know there's always exactly one activation in the parent's childIds
        // that needs to be transferred to this call tree node
        // in the above example, 1's child id would be first transferred from a to b and then from b
        // to c
        // this ensures that the UI knows that c is the parent of 1
        parent.giveLastChildIdTo(this);
      }

      List<CallTree> callTrees = getChildren();
      for (int i = 0, size = callTrees.size(); i < size; i++) {
        CallTree child = callTrees.get(i);
        child.activation(activeContextOfDirectParent, activationTimestamp);
        child.deactivationTimestamp = deactivationTimestamp;
        // re-run this logic for all children, even if they have already ended
        child.end(pool, minDurationNs, root);
      }
      activeContextOfDirectParent = null;
      activationTimestamp = -1;
      deactivationTimestamp = -1;
    }
    if (parent != null && isTooFast(minDurationNs)) {
      root.previousTopOfStack = parent;
      parent.removeChild(pool, this);
    } else {
      CallTree lastChild = getLastChild();
      if (lastChild != null && !lastChild.isEnded()) {
        lastChild.end(pool, minDurationNs, root);
      }
    }
  }

  private boolean isTooFast(long minDurationNs) {
    return count == 1 || isFasterThan(minDurationNs);
  }

  private void removeChild(ObjectPool<CallTree> pool, CallTree child) {
    children.remove(child);
    child.recursiveGiveChildIdsTo(this);
    child.recycle(pool);
  }

  private boolean isFasterThan(long minDurationNs) {
    return getDurationNs() < minDurationNs;
  }

  private boolean deactivationHappenedBeforeEnd() {
    return activeContextOfDirectParent != null
        && deactivationTimestamp > -1
        && lastSeen > deactivationTimestamp;
  }

  public boolean isLeaf() {
    return children.isEmpty();
  }

  /**
   * Returns {@code true} if this node has just one child and no self time.
   *
   * <pre>
   *  c
   *  b  ← b is a pillar
   * aaa
   * </pre>
   */
  private boolean isPillar() {
    return children.size() == 1 && children.get(0).count == count;
  }

  @Nullable
  public CallTree getLastChild() {
    return children.size() > 0 ? children.get(children.size() - 1) : null;
  }

  public boolean isEnded() {
    return ended;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    try {
      toString(sb);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return sb.toString();
  }

  private void toString(Appendable out) throws IOException {
    toString(out, 0);
  }

  private void toString(Appendable out, int level) throws IOException {
    for (int i = 0; i < level; i++) {
      out.append("  ");
    }
    out.append(frame != null ? frame.getClassName() : "null")
        .append('.')
        .append(frame != null ? frame.getMethodName() : "null")
        .append(' ')
        .append(Integer.toString(count))
        .append('\n');
    for (CallTree node : children) {
      node.toString(out, level + 1);
    }
  }

  int spanify(
      CallTree.Root root,
      @Nullable Span parentSpan,
      TraceContext parentContext,
      SpanAnchoredClock clock,
      BiConsumer<SpanBuilder, SpanContext> spanParentOverride,
      StringBuilder tempBuilder,
      Tracer tracer) {
    int createdSpans = 0;
    if (activeContextOfDirectParent != null) {
      parentSpan = null;
      parentContext = activeContextOfDirectParent;
    }
    Span span = null;
    if (!isPillar() || isLeaf()) {
      createdSpans++;
      span =
          asSpan(root, parentSpan, parentContext, tracer, clock, spanParentOverride, tempBuilder);
      this.isSpan = true;
    }
    List<CallTree> children = getChildren();
    for (int i = 0, size = children.size(); i < size; i++) {
      createdSpans +=
          children
              .get(i)
              .spanify(
                  root,
                  span != null ? span : parentSpan,
                  parentContext,
                  clock,
                  spanParentOverride,
                  tempBuilder,
                  tracer);
    }
    return createdSpans;
  }

  protected Span asSpan(
      Root root,
      @Nullable Span parentSpan,
      TraceContext parentContext,
      Tracer tracer,
      SpanAnchoredClock clock,
      BiConsumer<SpanBuilder, SpanContext> spanParentOverride,
      StringBuilder tempBuilder) {

    Context parentOtelCtx;
    if (parentSpan != null) {
      parentOtelCtx = Context.root().with(parentSpan);
    } else {
      tempBuilder.setLength(0);
      parentOtelCtx = Context.root().with(Span.wrap(parentContext.toOtelSpanContext(tempBuilder)));
    }

    tempBuilder.setLength(0);
    assert frame != null;
    String classFqn = frame.getClassName();
    if (classFqn != null) {
      tempBuilder.append(classFqn, frame.getSimpleClassNameOffset(), classFqn.length());
    } else {
      tempBuilder.append("null");
    }
    tempBuilder.append("#");
    tempBuilder.append(frame.getMethodName());

    transferMaybeChildIdsToChildIds();

    SpanBuilder spanBuilder =
        tracer
            .spanBuilder(tempBuilder.toString())
            .setParent(parentOtelCtx)
            .setAttribute(SPAN_IS_INFERRED, true)
            .setStartTimestamp(
                clock.toEpochNanos(parentContext.getClockAnchor(), this.start),
                TimeUnit.NANOSECONDS);
    insertChildIdLinks(
        spanBuilder,
        Span.fromContext(parentOtelCtx).getSpanContext(),
        parentContext,
        spanParentOverride,
        tempBuilder);

    // we're not interested in the very bottom of the stack which contains things like accepting and
    // handling connections
    if (parentSpan != null || !root.rootContext.idEquals(parentContext)) {
      // we're never spanifying the root
      assert this.parent != null;
      tempBuilder.setLength(0);
      this.parent.fillStackTrace(tempBuilder);
      spanBuilder.setAttribute(CODE_STACKTRACE, tempBuilder.toString());
    }

    Span span = spanBuilder.startSpan();
    span.end(
        clock.toEpochNanos(parentContext.getClockAnchor(), this.start + getDurationNs()),
        TimeUnit.NANOSECONDS);
    return span;
  }

  private void insertChildIdLinks(
      SpanBuilder span,
      SpanContext parentContext,
      TraceContext nonInferredParent,
      BiConsumer<SpanBuilder, SpanContext> spanParentOverride,
      StringBuilder tempBuilder) {
    if (childIds == null || childIds.isEmpty()) {
      return;
    }
    for (int i = 0; i < childIds.getSize(); i++) {
      // to avoid cycles, we only insert child-ids if the parent of the child is also
      // the parent of the stack of inferred spans inserted
      if (nonInferredParent.getSpanId() == childIds.getParentId(i)) {
        tempBuilder.setLength(0);
        HexUtils.appendLongAsHex(childIds.getId(i), tempBuilder);
        SpanContext childSpanContext =
            SpanContext.create(
                parentContext.getTraceId(),
                tempBuilder.toString(),
                parentContext.getTraceFlags(),
                parentContext.getTraceState());
        spanParentOverride.accept(span, childSpanContext);
      }
    }
  }

  /** Fill in the stack trace up to the parent span */
  private void fillStackTrace(StringBuilder resultBuilder) {
    if (parent != null && !this.isSpan) {
      if (resultBuilder.length() > 0) {
        resultBuilder.append('\n');
      }
      assert frame != null;
      resultBuilder
          .append("at ")
          .append(frame.getClassName())
          .append('.')
          .append(frame.getMethodName())
          .append('(');
      frame.appendFileName(resultBuilder);
      resultBuilder.append(')');
      parent.fillStackTrace(resultBuilder);
    }
  }

  /**
   * Recycles this subtree to the provided pool recursively. Note that this method ends by recycling
   * {@code this} node (i.e. - this subtree root), which means that <b>the caller of this method
   * should make sure that no reference to this object is held anywhere</b>.
   *
   * <p>ALSO NOTE: MAKE SURE NOT TO CALL THIS METHOD FOR {@link CallTree.Root} INSTANCES.
   *
   * @param pool the pool to which all subtree nodes are to be recycled
   */
  public final void recycle(ObjectPool<CallTree> pool) {
    assert !(this instanceof Root);
    List<CallTree> children = this.children;
    for (int i = 0, size = children.size(); i < size; i++) {
      children.get(i).recycle(pool);
    }
    pool.recycle(this);
  }

  @Override
  public void resetState() {
    parent = null;
    count = 0;
    frame = null;
    start = 0;
    lastSeen = 0;
    ended = false;
    activationTimestamp = -1;
    activeContextOfDirectParent = null;
    deactivationTimestamp = -1;
    isSpan = false;
    childIds = null;
    maybeChildIds = null;
    depth = 0;
    if (children.size() > INITIAL_CHILD_SIZE) {
      // the overwhelming majority of call tree nodes has either one or two children
      // don't let outliers grow all lists in the pool over time
      children = new ArrayList<>(INITIAL_CHILD_SIZE);
    } else {
      children.clear();
    }
  }

  /**
   * When a regular span is activated, we want it's {@code span.id} to be added to the call tree
   * that represents the {@linkplain CallTree.Root#topOfStack top of the stack} to ensure correct
   * parent/child relationships via re-parenting.
   *
   * <p>However, the {@linkplain CallTree.Root#topOfStack current top of the stack} may turn out to
   * not be the right target. Consider this example:
   *
   * <pre>
   * bb
   * aa aa
   *   1  1  ← activation
   * </pre>
   *
   * <p>We would add the id of span {@code 1} to {@code b}'s {@link #maybeChildIds}. But after
   * seeing the next frame, we realize the {@code b} has already ended and that we should {@link
   * #giveMaybeChildIdsTo} from {@code b} and give it to {@code a}. This logic is implemented in
   * {@link CallTree.Root#addStackTrace}. After seeing another frame of {@code a}, we know that
   * {@code 1} is really the child of {@code a}, so we {@link #transferMaybeChildIdsToChildIds()}.
   *
   * @param id the child span id to add to this call tree element
   */
  public void addMaybeChildId(long id, long parentId) {
    if (maybeChildIds == null) {
      maybeChildIds = new ChildList();
    }
    maybeChildIds.add(id, parentId);
  }

  public void addChildId(long id, long parentId) {
    if (childIds == null) {
      childIds = new ChildList();
    }
    childIds.add(id, parentId);
  }

  public boolean hasChildIds() {
    return (maybeChildIds != null && maybeChildIds.getSize() > 0)
        || (childIds != null && childIds.getSize() > 0);
  }

  public void recursiveGiveChildIdsTo(CallTree giveTo) {
    for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
      children.get(i).recursiveGiveChildIdsTo(giveTo);
    }
    giveChildIdsTo(giveTo);
    giveMaybeChildIdsTo(giveTo);
  }

  void giveChildIdsTo(CallTree giveTo) {
    if (this.childIds == null) {
      return;
    }
    if (giveTo.childIds == null) {
      giveTo.childIds = this.childIds;
    } else {
      giveTo.childIds.addAll(this.childIds);
    }
    this.childIds = null;
  }

  void giveLastChildIdTo(CallTree giveTo) {
    if (childIds != null && !childIds.isEmpty()) {
      int size = childIds.getSize();
      long id = childIds.getId(size - 1);
      long parentId = childIds.getParentId(size - 1);
      giveTo.addChildId(id, parentId);
      childIds.removeLast();
    }
  }

  void giveMaybeChildIdsTo(CallTree giveTo) {
    if (this.maybeChildIds == null) {
      return;
    }
    if (giveTo.maybeChildIds == null) {
      giveTo.maybeChildIds = this.maybeChildIds;
    } else {
      giveTo.maybeChildIds.addAll(this.maybeChildIds);
    }
    this.maybeChildIds = null;
  }

  public int getDepth() {
    return depth;
  }

  /**
   * A special kind of a {@link CallTree} node which represents the root of the call tree. This acts
   * as the interface to the outside to add new nodes to the tree or to update existing ones by
   * {@linkplain #addStackTrace adding stack traces}.
   */
  public static class Root extends CallTree implements Recyclable {
    private static final Logger logger = Logger.getLogger(Root.class.getName());
    private static final StackFrame ROOT_FRAME = new StackFrame("root", "root");

    /**
     * The context of the thread root, mostly a transaction or a span which got activated in an
     * auxiliary thread
     */
    protected TraceContext rootContext;

    /**
     * The context of the transaction or span which is currently active. This is lazily deserialized
     * from {@link #activeSpanSerialized} if there's an actual {@linkplain #addStackTrace stack
     * trace} for this activation.
     */
    @Nullable private TraceContext activeSpan;

    /** The timestamp of when {@link #activeSpan} got activated */
    private long activationTimestamp = -1;

    /**
     * The context of the transaction or span which is currently active, in its {@linkplain
     * TraceContext#serialize serialized} form.
     */
    private final byte[] activeSpanSerialized = new byte[TraceContext.SERIALIZED_LENGTH];

    @Nullable private CallTree previousTopOfStack;
    @Nullable private CallTree topOfStack;

    private final LongHashSet activeSet = new LongHashSet();

    public Root() {
      this.rootContext = new TraceContext();
    }

    private void set(byte[] traceContext, long nanoTime) {
      super.set(null, ROOT_FRAME, nanoTime);
      this.rootContext.deserialize(traceContext);
      setActiveSpan(traceContext, nanoTime);
    }

    public void setActiveSpan(byte[] activeSpanSerialized, long timestamp) {
      activationTimestamp = timestamp;
      System.arraycopy(
          activeSpanSerialized, 0, this.activeSpanSerialized, 0, activeSpanSerialized.length);
      this.activeSpan = null;
    }

    public void onActivation(byte[] active, long timestamp) {
      setActiveSpan(active, timestamp);
      if (topOfStack != null) {
        long spanId = TraceContext.getSpanId(active);
        activeSet.add(spanId);
        if (!isNestedActivation(topOfStack)) {
          topOfStack.addMaybeChildId(spanId, TraceContext.getParentId(active));
        }
      }
    }

    private boolean isNestedActivation(CallTree topOfStack) {
      return isAnyActive(topOfStack.childIds) || isAnyActive(topOfStack.maybeChildIds);
    }

    private boolean isAnyActive(@Nullable ChildList spanIds) {
      if (spanIds == null) {
        return false;
      }
      for (int i = 0, size = spanIds.getSize(); i < size; i++) {
        if (activeSet.contains(spanIds.getId(i))) {
          return true;
        }
      }
      return false;
    }

    public void onDeactivation(byte[] deactivated, byte[] active, long timestamp) {
      if (logger.isLoggable(FINE) && !Arrays.equals(activeSpanSerialized, deactivated)) {
        logger.log(WARNING, "Illegal state: deactivating span that is not active");
      }
      if (activeSpan != null) {
        handleDeactivation(activeSpan, activationTimestamp, timestamp);
      }
      // else: activeSpan has not been materialized because no stack traces were added during this
      // activation
      setActiveSpan(active, timestamp);
      // we're not interested in tracking nested activations that happen before we see the first
      // stack trace
      // that's because isNestedActivation is only called if topOfStack != null
      // this optimizes for the case where we have no stack traces for a fast executing transaction
      if (topOfStack != null) {
        long spanId = TraceContext.getSpanId(deactivated);
        activeSet.remove(spanId);
      }
    }

    public void addStackTrace(
        List<StackFrame> stackTrace,
        long nanoTime,
        ObjectPool<CallTree> callTreePool,
        long minDurationNs) {
      // only "materialize" trace context if there's actually an associated stack trace to the
      // activation
      // avoids allocating a TraceContext for very short activations which have no effect on the
      // CallTree anyway
      boolean firstFrameAfterActivation = false;
      if (activeSpan == null) {
        firstFrameAfterActivation = true;
        activeSpan = new TraceContext();
        activeSpan.deserialize(activeSpanSerialized);
      }
      previousTopOfStack = topOfStack;
      topOfStack =
          addFrame(
              stackTrace,
              stackTrace.size(),
              activeSpan,
              activationTimestamp,
              nanoTime,
              callTreePool,
              minDurationNs,
              this);

      // After adding the first frame after an activation, we can check if we added the child ids to
      // the correct CallTree
      // If the new top of stack is not a successor (a different branch vs just added nodes on the
      // same branch)
      // we have to transfer the child ids of not yet deactivated spans to the new top of the stack.
      // See also CallTreeTest.testActivationAfterMethodEnds and following tests.
      if (firstFrameAfterActivation
          && previousTopOfStack != topOfStack
          && previousTopOfStack != null
          && previousTopOfStack.hasChildIds()) {
        if (!topOfStack.isSuccessor(previousTopOfStack)) {
          CallTree commonAncestor = findCommonAncestor(previousTopOfStack, topOfStack);
          CallTree newParent = commonAncestor != null ? commonAncestor : topOfStack;
          if (newParent.count > 1) {
            previousTopOfStack.giveMaybeChildIdsTo(newParent);
          } else if (previousTopOfStack.maybeChildIds != null) {
            previousTopOfStack.maybeChildIds.clear();
          }
        }
      }
    }

    @Nullable
    private static CallTree findCommonAncestor(CallTree previousTopOfStack, CallTree topOfStack) {
      int maxDepthOfCommonAncestor = Math.min(previousTopOfStack.getDepth(), topOfStack.getDepth());
      CallTree commonAncestor = null;
      // i = 1 avoids considering the CallTree.Root node which is always the same
      for (int i = 1; i <= maxDepthOfCommonAncestor; i++) {
        CallTree ancestor1 = previousTopOfStack.getNthParent(previousTopOfStack.getDepth() - i);
        CallTree ancestor2 = topOfStack.getNthParent(topOfStack.getDepth() - i);
        if (ancestor1 == ancestor2) {
          commonAncestor = ancestor1;
        } else {
          break;
        }
      }
      return commonAncestor;
    }

    /**
     * Creates spans for call tree nodes if they are either not a {@linkplain #isPillar() pillar} or
     * are a {@linkplain #isLeaf() leaf}. Nodes which are not converted to {@link Span}s are part of
     * the span stackframes for the nodes which do get converted to a span.
     *
     * <p>Parent/child relationships with the regular spans are maintained. One exception is that an
     * inferred span can't be the parent of a regular span. That is because the regular spans have
     * already been reported once the inferred spans are created. In the future, we might make it
     * possible to update the parent ID of a regular span so that it correctly reflects being a
     * child of an inferred span.
     */
    public int spanify(
        SpanAnchoredClock clock,
        Tracer tracer,
        BiConsumer<SpanBuilder, SpanContext> normalSpanOverride) {
      StringBuilder tempBuilder = new StringBuilder();
      int createdSpans = 0;
      List<CallTree> callTrees = getChildren();
      for (int i = 0, size = callTrees.size(); i < size; i++) {
        createdSpans +=
            callTrees
                .get(i)
                .spanify(this, null, rootContext, clock, normalSpanOverride, tempBuilder, tracer);
      }
      return createdSpans;
    }

    public TraceContext getRootContext() {
      return rootContext;
    }

    /**
     * Recycles this tree to the provided pools. First, all child subtrees are recycled recursively
     * to the children pool. Then, {@code this} root node is recycled to the root pool. This means
     * that <b>the caller of this method should make sure that no reference to this root object is
     * held anywhere</b>.
     *
     * @param childrenPool object pool for all non-root nodes
     * @param rootPool object pool for root nodes
     */
    public void recycle(ObjectPool<CallTree> childrenPool, ObjectPool<CallTree.Root> rootPool) {
      List<CallTree> children = getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        children.get(i).recycle(childrenPool);
      }
      rootPool.recycle(this);
    }

    public void end(ObjectPool<CallTree> pool, long minDurationNs) {
      end(pool, minDurationNs, this);
    }

    @Override
    public void resetState() {
      super.resetState();
      rootContext.resetState();
      activeSpan = null;
      activationTimestamp = -1;
      Arrays.fill(activeSpanSerialized, (byte) 0);
      previousTopOfStack = null;
      topOfStack = null;
      activeSet.clear();
    }
  }
}
