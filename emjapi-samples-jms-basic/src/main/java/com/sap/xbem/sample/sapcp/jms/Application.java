package com.sap.xbem.sample.sapcp.jms;

import javax.jms.Message;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.sap.xbem.sample.sapcp.jms.Application.RunMode.*;

@SuppressWarnings("ALL")
public class Application {
  enum RunMode {SEND, RECEIVE, SEND_TOPIC, FULL};

  public static void main(String ... args) throws Exception {
    String propertiesFile = "em-connection.properties";
    if(args.length >= 1) {
      propertiesFile = args[0];
    }
    RunMode runMode = FULL;
    if(args.length >= 2) {
      runMode = RunMode.valueOf(args[1].toUpperCase(Locale.US));
    }

    List<Message> messages = new Application().run(propertiesFile, runMode);

    System.out.println("Received messages: ");
    messages.stream()
            .map(EmJmsBase.Utils::unwrapMessageText)
            .forEach(System.out::println);
  }

  public List<Message> run(String propertiesFilename, RunMode mode) throws Exception {
    if(mode == FULL || mode == SEND) {
      XbemJmsProducer producer = new XbemJmsProducer(propertiesFilename);
      producer.produceMessages();
      producer.close();
    } else if(mode == SEND_TOPIC) {
      XbemJmsProducer producer = new XbemJmsProducer(propertiesFilename);
      producer.sendToTopic(producer.getTopicName());
      producer.close();
    }

    if(mode == FULL || mode == RECEIVE) {
      XbemJmsConsumer consumer = new XbemJmsConsumer(propertiesFilename);
      List<Message> messages = consumer.readQueue();
      consumer.close();
      return messages;
    }
    return Collections.emptyList();
  }
}
