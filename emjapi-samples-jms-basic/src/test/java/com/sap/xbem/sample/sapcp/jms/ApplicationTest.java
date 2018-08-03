package com.sap.xbem.sample.sapcp.jms;


import org.junit.Test;

import javax.jms.Message;
import java.util.List;

public class ApplicationTest {

  @Test
  public void run() throws Exception {
    Application app = new Application();
    List<Message> messages = app.run("em-connection.properties", Application.RunMode.FULL);

    System.out.println("Received messages: ");
    messages.stream()
            .map(XbemJmsBase.Utils::unwrapMessageText)
            .forEach(System.out::println);
  }
}
