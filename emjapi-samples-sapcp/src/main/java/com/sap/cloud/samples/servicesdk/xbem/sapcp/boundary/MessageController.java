package com.sap.cloud.samples.servicesdk.xbem.sapcp.boundary;

import com.sap.cloud.samples.servicesdk.xbem.sapcp.service.MessageEvent;
import com.sap.cloud.samples.servicesdk.xbem.sapcp.service.MessageService;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  public List<MessageEvent> getMessages() {
    try {
      if(messageService.initReceiver()) {
        LOG.info(() -> "MessageService receiver is active.");
      }
    } catch (MessagingException e) {
      LOG.info(() -> "MessageService receiver init failed: " + e.getMessage());
    }
    return messageService.getReceivedMessageEvents();
  }

  @PostMapping(value = "/messages", consumes = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity sendMessage(@RequestBody String content) {
    try {
      messageService.sendMessage(new MessageEvent(content));
      return ResponseEntity.accepted().build();
    } catch (MessagingException e) {
      LOG.severe(() -> "MessageService send failed: " + e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping(value = "/messages", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> close() {
    try {
      int clearedMessagesCount = messageService.clearMessages();
      messageService.closeReceiver();
      return ResponseEntity.ok("{\"clearedMessagesCount\":" + clearedMessagesCount+ "}");
    } catch (MessagingException e) {
      LOG.severe(() -> "MessageService close failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
