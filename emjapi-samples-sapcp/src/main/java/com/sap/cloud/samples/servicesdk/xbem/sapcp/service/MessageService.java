package com.sap.cloud.samples.servicesdk.xbem.sapcp.service;

import com.sap.cloud.servicesdk.xbem.api.Message;
import com.sap.cloud.servicesdk.xbem.api.MessagingEndpoint;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import com.sap.cloud.servicesdk.xbem.api.MessagingService;
import com.sap.cloud.servicesdk.xbem.connector.sapcp.MessagingServiceInfoProperties;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.ServiceConnectorConfig;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
public final class MessageService {
  private static final ConcurrentMap<String, MessagingEndpoint> BINDING_2_ENDPOINT = new ConcurrentHashMap<>();
  private static final Logger LOG = Logger.getLogger(MessageService.class.getName());
  private static final String IN_QUEUE = "in_queue";
  private static final String OUT_QUEUE = "out_queue";

  private final List<MessageEvent> messageEvents = Collections.synchronizedList(new ArrayList<>());

  public boolean initReceiver() throws MessagingException {
    MessagingEndpoint inQueue = grant(IN_QUEUE);
    if(!inQueue.isReceiving()) {
      inQueue.receive("client::xbem::samples::sapcp", (stream) -> {
        stream.peek((m) -> LOG.info("Message received..."))
          .map(Message::getContent).map(MessageEvent::new).forEach(messageEvents::add);
      });
    }
    return inQueue.isReceiving();
  }

  public void closeReceiver() throws MessagingException {
    closeReceiver(IN_QUEUE);
  }

  public int clearMessages() {
    int clearedMessagesAmount = messageEvents.size();
    messageEvents.clear();
    return clearedMessagesAmount;
  }

  public List<MessageEvent> getReceivedMessageEvents() {
    List<MessageEvent> tmp = new ArrayList<>(messageEvents);
    return Collections.unmodifiableList(tmp);
  }

  public MessageEvent sendContent(String content) throws MessagingException {
    return sendMessage(new MessageEvent(content));
  }

  public MessageEvent sendMessage(MessageEvent event) throws MessagingException {
    grant(OUT_QUEUE).createMessage()
      .setContent(event.toJson().getBytes(StandardCharsets.UTF_8)).send();
    return event;
  }

  public String getState() {
    StringBuilder state = new StringBuilder("MessageService state: in_queue is '");
    MessagingEndpoint inQueue = BINDING_2_ENDPOINT.get(IN_QUEUE);
    if(inQueue == null) {
      state.append("not initialized");
    } else {
      state.append("initialized and ").append(inQueue.isReceiving()? "receiving": "not active");
    }
    state.append("'; registered endpoints '").append(BINDING_2_ENDPOINT.keySet()).append(" (")
      .append(BINDING_2_ENDPOINT.size()).append(")';");
    return state.toString();
  }

  private synchronized MessagingEndpoint grant(String binding) throws MessagingException {
    MessagingEndpoint endpoint = BINDING_2_ENDPOINT.get(binding);
    if(endpoint == null) {
      final Cloud cloud = new CloudFactory().getCloud();
      ServiceConnectorConfig config = MessagingServiceInfoProperties.init().finish();
      MessagingService messagingService = cloud.getSingletonServiceConnector(MessagingService.class, config);
      endpoint = messagingService.bind(binding).build();
      BINDING_2_ENDPOINT.put(binding, endpoint);
      LOG.info(() -> "Created endpoint and bound to " + binding);
    } else if(endpoint.isClosed()) {
      // remove and re-connect
      BINDING_2_ENDPOINT.remove(binding);
      return grant(binding);
    }
    return endpoint;
  }

  private synchronized void closeReceiver(String binding) throws MessagingException {
    MessagingEndpoint destination = BINDING_2_ENDPOINT.remove(binding);
    if(destination != null) {
      destination.close(1, TimeUnit.SECONDS);
      destination.close(true);
      LOG.info(() -> "Closed destination");
      BINDING_2_ENDPOINT.remove(binding);
    }
  }
}
