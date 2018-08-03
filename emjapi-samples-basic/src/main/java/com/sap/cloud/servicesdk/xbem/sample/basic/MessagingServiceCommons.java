package com.sap.cloud.servicesdk.xbem.sample.basic;

import com.sap.cloud.servicesdk.xbem.api.MessagingEnvironment;
import com.sap.cloud.servicesdk.xbem.api.MessagingException;
import com.sap.cloud.servicesdk.xbem.api.MessagingService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Commons MessagingService helper class used by sender and receiver.
 */
public class MessagingServiceCommons {

  public static MessagingService initService(String filename) throws MessagingException {
    try {
      String json = JsonHelper.readContentFromResource(filename);
      MessagingEnvironment env = MessagingEnvironment.fromJson(json).build();
      //
      return MessagingService.with(env)
              .addSetting(MessagingService.Setting.MESSAGE_RELIABLE_MODE.withValue("1"))
              .addSetting(MessagingService.Setting.SSL_NO_VERIFY.withValue(true))
              .addSetting(MessagingService.Setting.SENDER_TIMEOUT_MS.withValue(10_000))
              .create();
    } catch (IOException e) {
      throw new MessagingException("Unable to init from file '" + filename + "'.", e);
    }
  }

  private static class JsonHelper {
    static String readContentFromResource(String filename) throws IOException {
      Path p = Paths.get(filename);
      if(Files.isReadable(p)) {
        // read file
        try(FileInputStream is = new FileInputStream(p.toFile())) {
          byte[] tmp = new byte[is.available()];
          is.read(tmp, 0, tmp.length);
          return new String(tmp, StandardCharsets.UTF_8);
        }
      }

      try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
        if(is == null) {
          throw new IOException("Resource/file '" + filename + "' not found.");
        }
        byte[] tmp = new byte[is.available()];
        is.read(tmp, 0, tmp.length);
        return new String(tmp, StandardCharsets.UTF_8);
      }
    }
  }
}
