package com.sap.xbem.sample.sapcp.jms;

import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.util.Properties;

/**
 * Send test messages to the pre-created queue on the SAP Enterprise Messaging service.
 * For HowTo / Setup take a look into the README.adoc in the project root path.
 *
 * The queue used for messages must exist on the message broker.
 */
@SuppressWarnings("ALL")
public class XbemJmsProducer extends XbemJmsBase {

  private static final Logger LOG = LoggerFactory.getLogger(XbemJmsProducer.class);

  protected XbemJmsProducer(String url, Properties properties) {
    super(url, properties);
  }

  public XbemJmsProducer(Properties properties) {
    super(properties);
  }

  public XbemJmsProducer(String propertiesFilename) {
    super(Utils.readProperties(propertiesFilename));
  }

  public void produceMessages() throws JMSException, IOException, MessagingException {
    sendToQueue(getQueueName());
  }

  public void sendToQueue(String queueName) throws JMSException, IOException, MessagingException {
    if(!connect()) {
      throw new RuntimeException("Connection failed " + getLastConnectionException());
    }
    // all JMS related
    Queue queue = session.createQueue(normalizeQueueName(queueName));
    MessageProducer messageProducer = session.createProducer(queue);

    long start = System.currentTimeMillis();
    int count = 10;
    LOG.info("Start sending messages...");
    for (int i = 1; i <= count; i++) {
      TextMessage message = session.createTextMessage("Text_" + i + "!");
      messageProducer.send(message);

      if (i % 2 == 0) {
        LOG.debug("Sent message " + i);
      }
    }

    long finish = System.currentTimeMillis();
    long taken = finish - start;
    LOG.info("Sent " + count + " messages in " + taken + "ms");

    close();
  }

  public void sendToTopic(String topicName) throws JMSException, IOException, MessagingException {
    if(!connect()) {
      throw new RuntimeException("Connection failed " + getLastConnectionException());
    }

    Topic topic = session.createTopic(normalizeTopicName(topicName));
    MessageProducer messageProducer = session.createProducer(topic);

    long start = System.currentTimeMillis();
    int count = 10;
    LOG.info("Start sending messages to topic ({})...", topicName);
    for (int i = 1; i <= count; i++) {
      TextMessage message = session.createTextMessage("Text_for_topic_" + i + "!");
      messageProducer.send(message);

      if (i % 2 == 0) {
        LOG.debug("Sent message " + i);
      }
    }

    long finish = System.currentTimeMillis();
    long taken = finish - start;
    LOG.info("Sent " + count + " messages in " + taken + "ms");

    close();
  }
}
