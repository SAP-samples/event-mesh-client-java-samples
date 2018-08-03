package com.sap.cloud.servicesdk.xbem.sample.basic;

import com.sap.cloud.servicesdk.xbem.api.MessagingBinding;
import com.sap.cloud.servicesdk.xbem.api.MessagingEndpoint;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import com.sap.cloud.servicesdk.xbem.api.MessagingService;

import java.util.concurrent.TimeUnit;

/**
 * Run against before created Enterprise Messaging Service @SAP CP.
 *
 * To run this sample the credentials provided by the Enterprise Messaging Service @SAP CP must be inserted
 * into the 'resources/credentials.json' (default credentials location).
 *
 * Additional the before created queue name must be provided here in the sample (see variable 'queueName').
 *
 * Afterwards the main method can be run to send messages.
 */
public class MainSender {

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

    // Run messaging sample
    System.out.println("Start...");
    for (int i = 0; i < 10; i++) {
      System.out.println("Send Message " + i);
      destination.createMessage().setContent(("Message-" + i).getBytes()).send();
    }
    System.out.println("Close...");
    destination.close(5, TimeUnit.SECONDS);
  }
}
