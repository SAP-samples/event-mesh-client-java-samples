package com.sap.xbem.sample.sapcp.jms;

import com.sap.cloud.servicesdk.xbem.api.MessagingEnvironment;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import com.sap.cloud.servicesdk.xbem.api.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.IllegalStateException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Sends a persistent message to a queue using Apache Qpid JMS 2.0 API over AMQP 1.0. Solace Message Router is used
 * as the message broker.
 *
 * RabbitMQ docker: `docker run -it --rm --hostname my-rabbit -p 35672:5672 -p 31883:1883 -p 8082:15672 mibo/rabbitmq:mqtt_amqp10`
 *
 * The queue used for messages must exist on the message broker.
 */
@SuppressWarnings("ALL")
public abstract class EmJmsBase {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  public static final String PROPERTY_URL = "messaging.uri";
  public static final String PROPERTY_QUEUE_NAME = "em.jms.queue.name";
  private static final String PROPERTY_TOPIC_NAME = "em.jms.topic.name";
  public static final String QUEUE_NAME_PREFIX = "queue:";
  public static final String TOPIC_NAME_PREFIX = "topic:";

  protected final String url;
  protected final Properties properties;
  protected Connection connection;
  protected Session session;
  protected boolean connected = false;
  private JMSException lastConnectionException;

  protected EmJmsBase(String url, Properties properties) {
    this.url = url;
    this.properties = properties;
  }

  protected EmJmsBase(Properties properties) {
    this.url = properties.getProperty(PROPERTY_URL);
    this.properties = properties;
  }

  protected JMSException getLastConnectionException() {
    return lastConnectionException;
  }

  /**
   * Get the queue name from the properties.
   * If not configured an exception is thrown.
   * @return
   */
  protected String getQueueName() {
    String queueName = properties.getProperty(PROPERTY_QUEUE_NAME);
    if(queueName == null) {
      throw new IllegalStateException("Property " + PROPERTY_QUEUE_NAME + " is not configured");
    }
    return normalizeQueueName(queueName);
  }

  /**
   * Get the topic name from the properties.
   * If not configured an exception is thrown.
   * @return
   */
  protected String getTopicName() {
    String queueName = properties.getProperty(PROPERTY_TOPIC_NAME);
    if(queueName == null) {
      throw new IllegalStateException("Property " + PROPERTY_TOPIC_NAME + " is not configured");
    }
    return normalizeQueueName(queueName);
  }

  protected String normalizeQueueName(String queueName) {
    if(queueName.startsWith(QUEUE_NAME_PREFIX)) {
      return queueName;
    }
    return QUEUE_NAME_PREFIX + queueName;
  }

  protected String normalizeTopicName(String topicName) {
    if(topicName.startsWith(TOPIC_NAME_PREFIX)) {
      return topicName;
    }
    return TOPIC_NAME_PREFIX + topicName;
  }


  protected boolean connect() throws MessagingException, IOException {
    if(connected) {
      return true;
    }

    String credentialsJson = properties.getProperty("em.credentials.file");
    String json = Utils.readContentFromResource(credentialsJson);

    //
    MessagingService messagingService = MessagingService.with(
            MessagingEnvironment.fromJson(json).build())
//        .addSetting(MessagingService.Setting.MESSAGING_CONTEXT_IMPLEMENTATION.withValue(MessagingService.CONTEXT_ALIAS_JMS))
        .create();

    ConnectionFactory factory = messagingService.configure(ConnectionFactory.class);

    try {
      connection = factory.createConnection();
      connection.start();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      connected = true;
    } catch (JMSException e) {
      lastConnectionException = e;
    }
    return connected;
  }

  protected void close() throws JMSException {
    if(connected) {
      LOG.info("Closing session and connection.");
      connected = false;
      String exceptionMessage = "";
      try {
        session.close();
      } catch (JMSException e) {
        exceptionMessage = "Session close exception:: " + e.getMessage();
      }
      try {
        connection.close();
      } catch (JMSException e) {
        if(!exceptionMessage.isEmpty()) {
          exceptionMessage += ";\t";
        }
        exceptionMessage += "Connection close exception:: " + e.getMessage();
      }
      if(exceptionMessage.isEmpty()) {
        LOG.info("Closed successfully.");
      } else {
        throw new JMSException(exceptionMessage);
      }
    }
  }


  protected static class Utils {

    public static final int AMQP_TEXT_OFFSET = 5;
    static final String DEFAULT_PROPERTIES_FILE = "em-connection.properties";

    static Properties readProperties() {
      return readProperties(DEFAULT_PROPERTIES_FILE);
    }

    static Properties readProperties(String filename) {
      Properties p = new Properties();
      try(InputStream in = getFileAsStream(filename) ) {
        if(in != null) {
          p.load(in);
          return p;
        }
        throw new RuntimeException("Properties not found for file: " + filename);
      } catch (IOException e) {
        throw new RuntimeException("Unable to load properties from file: " + filename, e);
      }
    }

    /**
     * Get file with given name as stream.
     * Search order fro file location is
     * - current path
     * - parent path
     * - (context) class loader (getResourceAsStream())
     *
     * @param filename name of file
     * @return stream for given file
     * @throws IOException if file was not found
     */
    public static InputStream getFileAsStream(String filename) throws IOException {
      // first try root path
      Path path = Paths.get("./", filename);
      if (!path.toFile().exists()) {
        // second try parent root path
        path = Paths.get("../", filename);
        if (!path.toFile().exists()) {
          // at last try to get from class path
          final InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
          if (resource == null) {
            throw new IOException("Unable to locate file ('" + filename + "') from path or class loader.");
          }
          return resource;
        }
      }
      return new FileInputStream(path.toFile());
    }

    static String unwrapMessageText(Message message) {
      try {
        if(message instanceof TextMessage) {
          return ((TextMessage) message).getText();
        }
        if(message instanceof BytesMessage) {
          byte[] body = message.getBody(byte[].class);
          if(isAmqpPrefixed(body)) {
            int len = body.length - AMQP_TEXT_OFFSET;
            return new String(body, AMQP_TEXT_OFFSET, len, StandardCharsets.UTF_8);
          }
          return new String(body, StandardCharsets.UTF_8);
        }
      } catch (JMSException e) {
        return "Exception during message processing: " + e.getMessage();
      }
      return "Unknown Message Type";
    }

    private static boolean isAmqpPrefixed(byte[] body) {
      // 0 = 0 //1 = 83 //2 = 119 //3 = -95 //4 = 7
      if(body.length >= 5) {
        int len = body.length - 5;
        return body[0] == 0 && body[1] == 83 && body[2] == 119 && body[3] == -95 && body[4] == len;
      }
      return false;
    }

    public static String readContentFromResource(String filename) throws IOException {
      try(InputStream is = getFileAsStream(filename)) {
        byte[] tmp = new byte[is.available()];
        is.read(tmp, 0, tmp.length);
        return new String(tmp, StandardCharsets.UTF_8);
      }
    }
  }
}
