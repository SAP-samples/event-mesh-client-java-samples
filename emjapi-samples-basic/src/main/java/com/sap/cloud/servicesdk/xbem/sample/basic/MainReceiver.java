package com.sap.cloud.servicesdk.xbem.sample.basic;

import com.sap.cloud.servicesdk.xbem.api.MessagingBinding;
import com.sap.cloud.servicesdk.xbem.api.MessagingEndpoint;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import com.sap.cloud.servicesdk.xbem.api.MessagingService;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Run against before created Enterprise Messaging Service @SAP CP.
 *
 * To run this sample the credentials provided by the Enterprise Messaging Service @SAP CP must be inserted
 * into the 'resources/credentials.json' (default credentials location).
 *
 * Additional the before created queue name must be provided here in the sample (see variable 'queueName').
 *
 * Afterwards the main method can be run to receive messages.
 */
public class MainReceiver {

  public static void main(String ... args) throws MessagingException {
    // Main method setup
    String queueName = "NameOfQueue";
    if(args.length >= 1) {
      queueName = args[0];
    }
    String credentialsFilename = "credentials.json";
    if(args.length >= 2) {
      credentialsFilename = args[1];
    }

    // Messaging Service setup
    MessagingBinding binding = MessagingBinding.with("xbem-binding")
      .queue(queueName) // queue with given name must be created on message broker before the sender gets started
      .build();

    MessagingService messaging = MessagingServiceCommons.initService(credentialsFilename); // message broker must be started before
    MessagingEndpoint destination = messaging.bind(binding).build();

    // Run messaging sample - this will run util the JVM gets manually terminated by the user
    System.out.println("Start...");
    AtomicInteger counter = new AtomicInteger(0);
    destination.receive()
      .peek(m -> System.out.println("Received message: " + counter.incrementAndGet() + " content: " + m.toString() ))
      .map(m -> new String(m.getContent()))
      .forEach(System.out::println);
  }
}
