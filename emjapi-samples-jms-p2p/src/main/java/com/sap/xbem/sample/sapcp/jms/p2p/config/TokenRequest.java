package com.sap.xbem.sample.sapcp.jms.p2p.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cloud.servicesdk.xbem.core.MessagingService;
import com.sap.cloud.servicesdk.xbem.core.exception.MessagingRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TokenRequest {

    private final WebClient webClient;
    private final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

    public TokenRequest() {
        Cloud cloud = new CloudFactory().getCloud();
        MessagingService messagingServiceClientInfo = cloud.getSingletonServiceConnector(MessagingService.class, null);
        String endpoint = messagingServiceClientInfo.getOAuthTokenEndpoint();
        String clientId = messagingServiceClientInfo.getClientId();
        String clientSecret = messagingServiceClientInfo.getClientSecret();

        webClient = WebClient.builder().baseUrl(endpoint).build();

        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "client_credentials");
        formData.add("response", "token");
    }

    public String requestToken() {
        JsonNode tokenResponse = webClient.post().contentType(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData)).retrieve().bodyToMono(JsonNode.class).block();
        return tokenResponse.get("access_token").textValue();
    }
}
