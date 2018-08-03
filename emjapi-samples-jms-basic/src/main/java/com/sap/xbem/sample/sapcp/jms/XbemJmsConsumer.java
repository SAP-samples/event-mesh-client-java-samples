package com.sap.xbem.sample.sapcp.jms;

import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Register to the pre-created queue on the SAP Enterprise Messaging service an listen for messages.
 * For HowTo / Setup take a look into the README.adoc in the project root path.
 *
 * The queue used for messages must exist on the message broker.
 */
@SuppressWarnings("ALL")
public class XbemJmsConsumer extends XbemJmsBase {

  private static final Logger LOG = LoggerFactory.getLogger(XbemJmsConsumer.class);
  private static final String PROP_USE_MSG_LISTENER = "em.jms.consumer.msg.listener";
  private static final String PROP_USE_MSG_LISTENER_TIMEOUT = "em.jms.consumer.msg.listener.timeout";

  public XbemJmsConsumer(Properties properties) {
    super(properties);
  }

  protected XbemJmsConsumer(String url, Properties properties) {
    super(url, properties);
  }

  public XbemJmsConsumer(String propertiesFilename) {
    super(Utils.readProperties(propertiesFilename));
  }

  private List<Message> sampleReceiveAllMessages(Destination destination, boolean close) throws JMSException {
    connection.start();

    final MessageConsumer consumer = session.createConsumer(destination);
    long start = System.currentTimeMillis();

    List<Message> messageList = new ArrayList<>();
    while(true) {
      Message m = consumer.receiveNoWait();
      if(m == null) {
        break;
      }
      messageList.add(m);
      final byte[] body = m.getBody(byte[].class);
      final String content = new String(body, StandardCharsets.UTF_8);
      LOG.info("Received message: " + content);
      m.acknowledge();
    }

    long finish = System.currentTimeMillis();
    long taken = finish - start;
    LOG.info("Received " + messageList.size() + " messages in " + taken + "ms");
    if(close) {
      close();
    }
    return messageList;
  }

  private List<Message> sampleWithMessageListener(Destination destination, long receiveTimeInSeconds, boolean close) throws JMSException {
    List<Message> messageList = Collections.synchronizedList(new ArrayList<>());

    final MessageConsumer consumer = session.createConsumer(destination);
    consumer.setMessageListener((message) -> {
      try {
        final byte[] body = message.getBody(byte[].class);
        final String content = new String(body, StandardCharsets.UTF_8);
        LOG.info("Message received: " + content);
        messageList.add(message);
        message.acknowledge();
      } catch (JMSException e) {
        throw new RuntimeException("Unexpected exception during message receive: " + e.getMessage(), e);
      }
    });
    connection.start();

    LOG.info("Wait " + receiveTimeInSeconds + "seconds for further messages.");
    try {
      TimeUnit.SECONDS.sleep(receiveTimeInSeconds);
    } catch (InterruptedException e) {
      Thread.interrupted();
    }
    LOG.info("Received " + messageList.size() + " messages in " + receiveTimeInSeconds + "s");
    if(close) {
      consumer.close();
      close();
    }

    return messageList;
  }

  public List<Message> readQueue() throws JMSException, IOException, MessagingException {
    return readQueue(getQueueName());
  }

  public List<Message> readQueue(String queueName) throws JMSException, IOException, MessagingException {
    if(connect()) {
      Queue queue = session.createQueue(normalizeQueueName(queueName));
      if(useMessageListener()) {
        return sampleWithMessageListener(queue, messageListenerTimeout(), true);
      }
      return sampleReceiveAllMessages(queue, true);
    }
    throw new RuntimeException("Failed to connect:: " + getLastConnectionException(), getLastConnectionException());
  }

  private int messageListenerTimeout() {
    return Integer.parseInt(properties.getProperty(PROP_USE_MSG_LISTENER_TIMEOUT, "5"));
  }
  private boolean useMessageListener() {
    return Boolean.parseBoolean(properties.getProperty(PROP_USE_MSG_LISTENER, "true"));
  }
}
