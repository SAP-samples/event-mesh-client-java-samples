package com.sap.xbem.sample.sapcp.jms.p2p.services;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.servicesdk.xbem.core.exception.MessagingException;
import com.sap.cloud.servicesdk.xbem.extension.sapcp.jms.MessagingServiceJmsConnectionFactory;

@RestController(MessagingServiceRestController.ROOT_PATH)
public class MessagingServiceRestController {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceRestController.class);
    public static final String ROOT_PATH = "/";
    private static final String QUEUE_PATH = "queue/{queueName}";
    private static final String MESSAGE_PATH = "/message";
    private static final String ENCODED_PATH = "encode";
    private static final String MESSAGE_REST_PATH = QUEUE_PATH + MESSAGE_PATH;
    private static final String MESSAGE_ENCODING_REST_PATH = ENCODED_PATH;

    private static final String QUEUE_PREFIX = "queue:"; // mandatory prefix for connection to a queue. Note that you must not create a queue on the broker with this prefix

    private final MessagingServiceJmsConnectionFactory connectionFactory;

    @Autowired
    private MessagingServiceRestController(MessagingServiceJmsConnectionFactory messagingServiceJmsConnectionFactory) {
        this.connectionFactory = messagingServiceJmsConnectionFactory;
    }

    /**
     * Convenient method to encode a value.
     * 
     * @param value
     *            value to encode
     * @return encoded value
     * @throws UnsupportedEncodingException
     */
    @PostMapping(MESSAGE_ENCODING_REST_PATH)
    public ResponseEntity<String> encodeValue(@RequestBody String value) throws UnsupportedEncodingException {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        return new ResponseEntity<String>(encodedValue, HttpStatus.OK);
    }

    /**
     * Send a message to a queue. The queue must be created via e.G. the
     * Dashboard of the enterprise-messaging-service first.
     * 
     * @param message
     * @param queueName
     * @return the message and the topic which has been sent
     * @throws MessagingException
     */
    @PostMapping(MESSAGE_REST_PATH)
    public ResponseEntity<String> sendMessage(@RequestBody String message, @PathVariable String queueName) throws MessagingException {
        try {
            queueName = decodeValue(queueName);
        } catch (UnsupportedEncodingException e1) {
            return ResponseEntity.badRequest().body("Unable to decode the queuename");
        }

        LOG.info("Sending message={} to queue={}", message, queueName);
        try (Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            BytesMessage byteMessage = session.createBytesMessage();
            byteMessage.writeBytes(message.getBytes());
            Queue queue = session.createQueue(QUEUE_PREFIX + queueName); // even though the JMS API is "createQueue" the queue will not be created on the message broker
            MessageProducer producer = session.createProducer(queue);
            producer.send(byteMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body("message=" + message + " sent to queue=" + queueName);
        } catch (JMSException e) {
            LOG.error("Could not send message={}.", message, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not send message. Error=" + e);
        }
    }

    /**
     * Receives a message from a queue. The rest call will block until a message
     * is received.
     * 
     * @param queueName
     * @return the message which has been received
     * @throws MessagingException
     */
    @GetMapping(MESSAGE_REST_PATH)
    public ResponseEntity<String> receiveMessage(@PathVariable String queueName) throws MessagingException {
        try {
            queueName = decodeValue(queueName);
        } catch (UnsupportedEncodingException e1) {
            return ResponseEntity.badRequest().body("Unable to decode the queuename");
        }

        /*
         * create the connection and the session, don't forget to close those
         * resources if you're not using autoclosable
         * 
         * you can also switch the acknowledgment mode in the session to e.G.
         * Session.CLIENT_ACKNOWLEDGE. Remember to acknowledge the message by
         * yourself then: byteMessage.acknowledge().
         */
        try (Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) { // see comments above
            connection.start();
            Queue queue = session.createQueue(QUEUE_PREFIX + queueName); // see comments above
            MessageConsumer consumer = session.createConsumer(queue);
            BytesMessage message = (BytesMessage) consumer.receive(); // Blocking call. You can either define a timeout or use a message listener
            byte[] byteData = new byte[(int) message.getBodyLength()];
            message.readBytes(byteData);
            return ResponseEntity.ok(new String(byteData));
        } catch (JMSException e) {
            LOG.error("Could not receive message.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not receive message. Error=" + e);
        }
    }

    private String decodeValue(String value) throws UnsupportedEncodingException {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
    }
}
