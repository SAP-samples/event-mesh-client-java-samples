package com.sap.cloud.samples.servicesdk.xbem.sapcp.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MessageEvent {
  final String uuid;
  final String message;
  final Date timestamp;

  public MessageEvent(byte[] content) {
    this(new String(content), new Date());
  }

  public MessageEvent(String content) {
    this(content, new Date());
  }

  public MessageEvent(String message, Date timestamp) {
    this.message = message;
    this.timestamp = timestamp;
    this.uuid = UUID.randomUUID().toString();
  }

  public String getMessage() {
    return message;
  }

  public String getTimestamp() {
    if(timestamp != null) {
      SimpleDateFormat sdf = new SimpleDateFormat();
      return sdf.format(timestamp);
    }
    return "<no_time_set>";
  }

  public String getId() {
    return uuid;
  }

  public String toJson() {
    return "{\"id\": " + uuid + "\","
          + "\"message\": " + message + "\","
          + "\"timestamp\": \"" + getTimestamp()
          + "\"}";
  }
}