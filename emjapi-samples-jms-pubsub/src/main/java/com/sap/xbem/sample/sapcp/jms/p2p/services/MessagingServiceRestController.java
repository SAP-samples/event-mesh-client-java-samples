package com.sap.xbem.sample.sapcp.jms.p2p.services;

import com.sap.cloud.servicesdk.xbem.extension.sapcp.jms.MessagingServiceJmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.jms.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController(MessagingServiceRestController.ROOT_PATH)
public class MessagingServiceRestController {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceRestController.class);
    public static final String ROOT_PATH = "/";
    private static final String TOPIC_PATH = "topic/{topicName}/";
    private static final String QUEUE_PATH = "queue/{queueName}/";
    private static final String MESSAGE_PATH = "message";
    private static final String ENCODED_PATH = "encode";
    private static final String MESSAGE_TOPIC_REST_PATH = TOPIC_PATH + MESSAGE_PATH;
    private static final String MESSAGE_QUEUE_REST_PATH = QUEUE_PATH + MESSAGE_PATH;
    private static final String MESSAGE_ENCODING_REST_PATH = ENCODED_PATH;

    private static final String TOPIC_PREFIX = "topic:"; // mandatory prefix to bind a topic.
    private static final String QUEUE_PREFIX = "queue:"; // mandatory prefix to bind a queue. Note that you must not create a queue on the broker with this prefix!

    private final MessagingServiceJmsConnectionFactory connectionFactory;

    @Autowired
    private MessagingServiceRestController(MessagingServiceJmsConnectionFactory messagingServiceJmsConnectionFactory) {
        this.connectionFactory = messagingServiceJmsConnectionFactory;
    }

    /**
     * Convenient method to encode a value.
     *
     * @param value value to encode
     * @return encoded value
     */
    @PostMapping(MESSAGE_ENCODING_REST_PATH)
    public ResponseEntity<String> encodeValue(@RequestBody String value) throws UnsupportedEncodingException {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        return new ResponseEntity<>(encodedValue, HttpStatus.OK);
    }

    /**
     * Publishes a message to a given topic.
     *
     * @param message   to publish
     * @param topicName where message will be published
     * @return the message and the topic which has been sent
     */
    @PostMapping(MESSAGE_TOPIC_REST_PATH)
    public ResponseEntity<String> sendMessage(@RequestBody String message, @PathVariable String topicName) {
        try {
            topicName = decodeValue(topicName);
        } catch (UnsupportedEncodingException e1) {
            return ResponseEntity.badRequest().body("Unable to decode the queuename");
        }

        LOG.info("Sending message={} to topic={}", message, topicName);
        try (Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Topic topic = session.createTopic(TOPIC_PREFIX + topicName);
            BytesMessage byteMessage = session.createBytesMessage();
            byteMessage.writeBytes(message.getBytes());
            MessageProducer producer = session.createProducer(topic);
            producer.send(byteMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body("message=" + message + " sent to topic=" + topicName);
        } catch (JMSException e) {
            LOG.error("Could not send message={} to topic={}.", message, topicName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not send message. Error=" + e);
        }
    }

    /**
     * Receives a message from a queue. This example is supposed to be a publish
     * and subscribe scenario. Please create a queue subscription via e.G. the
     * Dashboard first.
     *
     * @param queueName name of the queue where message will receive
     * @return the message which has been received
     */
    @GetMapping(MESSAGE_QUEUE_REST_PATH)
    public ResponseEntity<String> receiveMessageFromQueue(@PathVariable String queueName) {
        try {
            queueName = decodeValue(queueName);
        } catch (UnsupportedEncodingException e1) {
            return ResponseEntity.badRequest().body("Unable to decode the queuename");
        }
        /*
         * create connection and session, don't forget to close those resources
         * if you're not using autoclosable
         *
         * you can also switch the acknowledgment mode in the session to e.G.
         * Session.CLIENT_ACKNOWLEDGE. Remember to acknowledge the message by
         * yourself then: byteMessage.acknowledge().
         */
        try (Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) { // see comments above
            connection.start();
            // even though the JMS API is "createQueue" the queue will not be created on the message broker
            Queue queue = session.createQueue(QUEUE_PREFIX + queueName);
            MessageConsumer consumer = session.createConsumer(queue);
            BytesMessage message = (BytesMessage) consumer.receive(); // Blocking call. You can either define a timeout or use a message listener
            byte[] byteData = new byte[(int) message.getBodyLength()];
            message.readBytes(byteData);
            return ResponseEntity.ok(new String(byteData));
        } catch (JMSException e) {
            LOG.error("Could not receive message.", e);
            LOG.error("Could not receive message from queue={}.", queueName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not receive message from queue. Error=" + e);
        }
    }

    private String decodeValue(String value) throws UnsupportedEncodingException {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
    }
}
