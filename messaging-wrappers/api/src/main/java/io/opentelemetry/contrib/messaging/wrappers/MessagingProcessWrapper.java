package io.opentelemetry.contrib.messaging.wrappers;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessResponse;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagingProcessWrapper<REQUEST extends MessagingProcessRequest, RESPONSE extends MessagingProcessResponse<?>> {

  private static final Logger LOG = Logger.getLogger(MessagingProcessWrapper.class.getName());

  private static final String INSTRUMENTATION_SCOPE = "messaging-process-wrapper";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private static final String OPERATION_NAME = "process";

  private final TextMapPropagator textMapPropagator;

  private final Tracer tracer;

  private final TextMapGetter<REQUEST> textMapGetter;

  private final List<MessagingSpanCustomizer<REQUEST, RESPONSE>> spanCustomizers;

  public Runnable wrap(REQUEST request, Runnable runnable) {
    return () -> {
      Span span = handleStart(request);
      Scope scope = span.makeCurrent();

      try {
        runnable.run();
      } catch (Throwable t) {
        handleEnd(span, request, null, t);
        scope.close();
        throw t;
      }

      handleEnd(span, request, null, null);
      scope.close();
    };
  }

  public <R> Callable<R> wrap(REQUEST request, Callable<R> callable) {
    return () -> {
      Span span = handleStart(request);
      Scope scope = span.makeCurrent();
      RESPONSE response = null;

      R result = null;
      try {
        result = callable.call();
        if (result instanceof MessagingProcessResponse) {
          response = (RESPONSE) result;
        }
      } catch (Throwable t) {
        handleEnd(span, request, response, t);
        scope.close();
        throw t;
      }

      handleEnd(span, request, response, null);
      scope.close();
      return result;
    };
  }

  public <R> R doProcess(REQUEST request, Callable<R> process) throws Exception {
    Span span = handleStart(request);
    Scope scope = span.makeCurrent();
    RESPONSE response = null;

    R result = null;
    try {
      result = process.call();
      if (result instanceof MessagingProcessResponse) {
        response = (RESPONSE) result;
      }
    } catch (Throwable t) {
      handleEnd(span, request, response, t);
      scope.close();
      throw t;
    }

    // noop response by default
    handleEnd(span, request, response, null);
    scope.close();
    return result;
  }

  protected Span handleStart(REQUEST request) {
    Context context = this.textMapPropagator.extract(Context.current(), request, this.textMapGetter);
    SpanBuilder spanBuilder = this.tracer.spanBuilder(getDefaultSpanName(request.getDestination()));
    spanBuilder.setParent(context);
    for (MessagingSpanCustomizer<REQUEST, RESPONSE> customizer : spanCustomizers) {
      try {
        context = customizer.onStart(spanBuilder, context, request);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Exception occurred while customizing span on start.", e);
      }
    }
    return spanBuilder.startSpan();
  }

  protected void handleEnd(Span span, REQUEST request, RESPONSE response, Throwable t) {
    for (MessagingSpanCustomizer<REQUEST, RESPONSE> customizer : spanCustomizers) {
      try {
        customizer.onEnd(span, Context.current(), request, response, t);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Exception occurred while customizing span on end.", e);
      }
    }
    span.end();
  }

  protected String getDefaultSpanName(String destination) {
    if (destination == null) {
      destination = "unknown";
    }
    return OPERATION_NAME + " " + destination;
  }

  protected MessagingProcessWrapper(OpenTelemetry openTelemetry,
                          TextMapGetter<REQUEST> textMapGetter,
                          List<MessagingSpanCustomizer<REQUEST, RESPONSE>> spanCustomizers) {
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
    this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE + "-" + INSTRUMENTATION_VERSION);
    this.textMapGetter = textMapGetter;
    this.spanCustomizers = spanCustomizers;
  }
}
