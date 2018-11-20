package com.sap.cloud.samples.servicesdk.xbem.sapcp.boundary;

import com.sap.cloud.samples.servicesdk.xbem.sapcp.service.MessageEvent;
import com.sap.cloud.samples.servicesdk.xbem.sapcp.service.MessageService;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/")
public class MessageController {

  private static final Logger LOG = Logger.getLogger(MessageController.class.getName());
  private MessageService messageService;

  public MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  @GetMapping("/health")
  public ResponseEntity healthCheck() {
    return ResponseEntity.ok("Sample running, " + messageService.getState());
  }

  @GetMapping(value = "/messages", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<MessageEvent> listenAndGetMessages() {
    try {
      if(messageService.initReceiver()) {
        LOG.info(() -> "MessageService receiver is active.");
      }
    } catch (MessagingException e) {
      LOG.info(() -> "MessageService receiver init failed: " + e.getMessage());
    }
    return messageService.getReceivedMessageEvents();
  }

  @GetMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<MessageEvent> getMessages() throws MessagingException {
    try {
      LOG.info(() -> "Start MessageService receiver...");
      List<MessageEvent> messages = messageService.receiveMessages();
      LOG.info(() -> "MessageService received " + messages.size() + " messages.");
      return messages;
    } catch (MessagingException e) {
      LOG.info(() -> "MessageService receiver init failed: " + e.getMessage());
      throw new MessagingException(e);
    }
  }

  @PostMapping(value = "/messages", consumes = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity sendMessage(@RequestBody String content) {
    try {
      final MessageEvent event = new MessageEvent(content);
      messageService.sendMessage(event);
      return ResponseEntity.accepted().body(event);
    } catch (MessagingException e) {
      LOG.severe(() -> "MessageService send failed: " + e.getMessage());
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @DeleteMapping(value = "/messages", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> clearReceivedMessages() {
    int clearedMessagesCount = messageService.clearMessages();
    LOG.info(() -> "MessageService cleared " + clearedMessagesCount + " messages.");
    return ResponseEntity.ok("{\"clearedMessagesCount\":" + clearedMessagesCount+ "}");
  }
}
