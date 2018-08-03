package com.sap.cloud.servicesdk.xbem.sample.basic;

import com.sap.cloud.servicesdk.xbem.api.MessagingException;

import java.util.Arrays;

public class MainApp {

  public static void main(String... args) throws MessagingException {
    if(args.length >= 1) {
      String action = args[0];
      if("send".equalsIgnoreCase(action)) {
        MainSender.main(Arrays.copyOfRange(args, 1, args.length));
        return;
      } else if("receive".equalsIgnoreCase(action)) {
        MainReceiver.main(Arrays.copyOfRange(args, 1, args.length));
        return;
      }
    }
    // unknown
    System.out.println("Unknown action. Call this main with first parameter 'send' or 'receive'.");
    System.out.println("Additional parameters are used as follows:.");
    System.out.println("Second parameter as: 'queue-name'.");
    System.out.println("Third parameter as: 'credentials file path'.");
  }
}