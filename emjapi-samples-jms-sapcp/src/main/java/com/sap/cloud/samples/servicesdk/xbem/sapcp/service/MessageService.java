package com.sap.cloud.samples.servicesdk.xbem.sapcp.service;

import com.sap.cloud.servicesdk.xbem.api.MessagingBinding;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import com.sap.cloud.servicesdk.xbem.api.MessagingService;
import com.sap.cloud.servicesdk.xbem.connector.sapcp.MessagingServiceInfoProperties;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.ServiceConnectorConfig;
import org.springframework.stereotype.Service;

import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Service
public final class MessageService {
  private static final Logger LOG = Logger.getLogger(MessageService.class.getName());
  private static final String IN_QUEUE = "in_queue";
  private static final String OUT_QUEUE = "out_queue";

  private final List<MessageEvent> messageList = Collections.synchronizedList(new ArrayList<>());

  private MessageConsumer messageListener;
  private MessagingService messagingService = null;

  public boolean initReceiver() throws MessagingException {
    try {
      if(messageListener == null) {
        Connection connection = grantConnection();
        messageListener = grantConsumer(connection.createSession(), IN_QUEUE);
        messageListener.setMessageListener((message) -> {
          try {
            final String content = extractBody(message);
            LOG.info("Message received: " + content);
            messageList.add(MessageEvent.parse(content));
            message.acknowledge();
          } catch (JMSException e) {
            throw new RuntimeException("Unexpected exception during message receive: " + e.getMessage(), e);
          }
        });
        messageList.clear();
        connection.start();
        LOG.info(() -> "MessageListener created...");
        return true;
      }
      LOG.info(() -> "MessageListener available...");
      return true;
    } catch (JMSException e) {
      throw new MessagingException(e);
    }
  }

  public List<MessageEvent> receiveMessages() throws MessagingException {
    if(messageListener != null) {
      try {
        messageListener.close();
        messageListener = null;
        LOG.info(() -> "MessageListener found; close for receiving messages...");
      } catch (JMSException e) {
        throw new MessagingException(e);
      }
    }
    List<MessageEvent> messageList = new ArrayList<>();
    try(Connection connection = grantConnection(); Session session = connection.createSession()) {
      final MessageConsumer messageConsumer = grantConsumer(session, IN_QUEUE);
      connection.start();
      while(true) {
        LOG.info("Start receiving messages: " + IN_QUEUE);
        Message m = messageConsumer.receiveNoWait();
        if(m == null) {
          LOG.info("Receiving no messages: " + IN_QUEUE);
          break;
        }
        final String content = extractBody(m);
        messageList.add(MessageEvent.parse(content));
        LOG.info("Received message: " + content);
        m.acknowledge();
      }
      return messageList;
    } catch (JMSException e) {
      if(messageList.isEmpty()) {
        throw new MessagingException(e);
      }
      return messageList;
    }
  }

  private String extractBody(Message m) throws JMSException {
    final byte[] body = m.getBody(byte[].class);
    return new String(body, 5, body.length-5, StandardCharsets.UTF_8);
  }

  public int clearMessages() {
    int clearedMessagesAmount = messageList.size();
    messageList.clear();
    return clearedMessagesAmount;
  }

  public List<MessageEvent> getReceivedMessageEvents() {
    List<MessageEvent> tmp = new ArrayList<>(messageList);
    return Collections.unmodifiableList(tmp);
  }

  public MessageEvent sendMessage(MessageEvent event) throws MessagingException {
    try {
      Session s = grantConnection().createSession();
      TextMessage message = s.createTextMessage(event.toScsv());
      MessageProducer producer = grantProducer(s, OUT_QUEUE);
      producer.send(message);
      LOG.info(() -> "Send message: " + event);
      return event;
    } catch (JMSException e) {
      throw new MessagingException(e);
    }
  }

  public String getState() {
    StringBuilder state = new StringBuilder("MessageService state is '");
    if(messagingService == null) {
      state.append("not initialized");
    } else {
      state.append("initialized");
    }
    return state.toString();
  }

  private MessageConsumer grantConsumer(Session s, String binding) throws JMSException {
    LOG.info(() -> "Grant consumer for binding " + binding);
    MessagingBinding b = grantMessagingService().getConfig().getBinding(binding);
    LOG.info(() -> "Grant consumer for endpoint " + b.getEndpointName() + " and address " + b.getAddress());
    Destination destination = s.createQueue(b.getAddress());
    MessageConsumer consumer = s.createConsumer(destination);
    LOG.info(() -> "Created consumer and bound to " + binding);
    return consumer;
  }

  private MessageProducer grantProducer(Session s, String binding) throws JMSException {
    LOG.info(() -> "Grant producer for binding " + binding);
    MessagingBinding b = grantMessagingService().getConfig().getBinding(binding);
    LOG.info(() -> "Grant producer for endpoint " + b.getEndpointName() + " and address " + b.getAddress());
    Destination destination = s.createQueue(b.getAddress());
    MessageProducer producer = s.createProducer(destination);
    LOG.info(() -> "Created producer for to " + binding);
    return producer;
  }

  private Connection grantConnection() throws MessagingException, JMSException {
    MessagingService messagingService = grantMessagingService();
    Connection connection = messagingService.configure(JmsConnectionFactory.class).createConnection();
    LOG.info(() -> "Created connection and cached it.");
    return connection;
  }

  private synchronized MessagingService grantMessagingService() {
    if(messagingService == null) {
      final Cloud cloud = new CloudFactory().getCloud();
      ServiceConnectorConfig config = MessagingServiceInfoProperties.init().finish();
      messagingService = cloud.getSingletonServiceConnector(MessagingService.class, config);
    }
    return messagingService;
  }
}
