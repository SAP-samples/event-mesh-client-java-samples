package com.sap.cloud.samples.servicesdk.xbem.sapcp.service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

  MessageEvent(String message, long timestamp, String id) {
    this.message = message;
    this.timestamp = new Date(timestamp);
    this.uuid = id;
  }

  public static MessageEvent parse(String content) {
    if(content.contains(";")) {
      String[] sp = content.split(";");
      Map<String, String> para2Val = Arrays.stream(sp).collect(
              Collectors.toMap((s) -> s.split("=")[0], (s) -> s.split("=")[1]));

      return new MessageEvent(
              para2Val.get("message"),
              Long.parseLong(para2Val.get("timestamp")),
              para2Val.get("id"));
    }
    return new MessageEvent(content);
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

  public String toScsv() {
    return "message=" + message + ";"
            + "id=" + uuid + ";"
            + "timestamp=" + timestamp.getTime() + ";";
  }

  public String toJson() {
    return "{\"id\": " + uuid + "\","
          + "\"message\": " + message + "\","
          + "\"timestamp\": \"" + getTimestamp()
          + "\"}";
  }
}