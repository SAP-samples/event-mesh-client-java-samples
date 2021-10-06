package com.sap.xbem.sample.sapcp.jms.p2p.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cloud.servicesdk.xbem.core.MessagingService;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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
        JsonNode tokenResponse = webClient.post().contentType(MediaType.APPLICATION_FORM_URLENCODED).accept(
                MediaType.APPLICATION_JSON).body(BodyInserters.fromFormData(formData)).retrieve().bodyToMono(JsonNode.class).block();
        return tokenResponse.get("access_token").textValue();
    }
}
