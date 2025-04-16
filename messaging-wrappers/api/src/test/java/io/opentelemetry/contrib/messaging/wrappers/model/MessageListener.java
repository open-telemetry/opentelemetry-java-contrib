package io.opentelemetry.contrib.messaging.wrappers.model;

import com.google.common.eventbus.Subscribe;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.messaging.wrappers.impl.MessageRequest;
import io.opentelemetry.contrib.messaging.wrappers.MessagingProcessWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.opentelemetry.contrib.messaging.wrappers.TestConstants.CLIENT_ID;
import static io.opentelemetry.contrib.messaging.wrappers.TestConstants.EVENTBUS_NAME;

public class MessageListener {

  private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);

  private final Tracer tracer;

  private final MessagingProcessWrapper<MessageRequest> wrapper;

  public static MessageListener create(Tracer tracer, MessagingProcessWrapper<MessageRequest> wrapper) {
    return new MessageListener(tracer, wrapper);
  }

  @Subscribe
  public void handleEvent(Message event) {
    wrapper.doProcess(MessageRequest.of(event, CLIENT_ID, EVENTBUS_NAME), () -> {
      Span span = tracer.spanBuilder("process child").startSpan();
      logger.info("Received event from <" + EVENTBUS_NAME + ">: " + event.getId());
      span.end();
    });
  }

  private MessageListener(Tracer tracer, MessagingProcessWrapper<MessageRequest> wrapper) {
    this.tracer = tracer;
    this.wrapper = wrapper;
  }
}
