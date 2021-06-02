package com.sap.xbem.sample.sapcp.jms.p2p.config;

import com.sap.cloud.servicesdk.xbem.core.MessagingService;
import com.sap.cloud.servicesdk.xbem.core.exception.MessagingRuntimeException;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;

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

    public static String requestToken() {
        Cloud cloud = new CloudFactory().getCloud();
        MessagingService messagingServiceClientInfo = cloud.getSingletonServiceConnector(MessagingService.class, null);
        String endpoint = messagingServiceClientInfo.getOAuthTokenEndpoint();
        String clientId = messagingServiceClientInfo.getClientId();
        String clientSecret = messagingServiceClientInfo.getClientSecret();
        OAuthHandler.AuthSettings oaSettings = OAuthHandler.AuthSettings.createClientCredentialsGrant(endpoint, clientId, clientSecret);

        try {
            OAuthHandler handler = new OAuthHandler(oaSettings);
            return handler.doTokenRequest();
        } catch (IOException e) {
            throw new MessagingRuntimeException("Unable to get OAuth token: " + e.getMessage(), e);
        }
    }

    static class OAuthHandler {

        private final OAuthHandler.AuthSettings authSettings;

        OAuthHandler(OAuthHandler.AuthSettings authSettings) {
            this.authSettings = authSettings;
        }

        String doTokenRequest() throws IOException {
            String oauthFlow = this.authSettings.getoAuthGrantType();
            if ("password".equalsIgnoreCase(oauthFlow)) {
                throw new UnsupportedOperationException("Found unknown oauth flow value: " + oauthFlow);
            } else if ("client_credentials".equalsIgnoreCase(oauthFlow)) {
                String url = this.authSettings.getoAuthEndpoint();
                String clientId = this.authSettings.getoAuthClientId();
                String clientSecret = this.authSettings.getoAuthClientSecret();
                return this.doTokenRequestClientFlow(url, clientId, clientSecret);
            } else {
                throw new IllegalStateException("Found unknown oauth flow value: " + oauthFlow);
            }
        }

        private String doTokenRequestClientFlow(String url, String clientId, String clientSecret) throws IOException {
            String body = String
                    .format("client_id=%s&client_secret=%s&grant_type=client_credentials&response_type=token", clientId, clientSecret);
            Map<String, String> headers = new HashMap();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Accept", "application/json");
            headers.put("User-Agent", "emjapi");
            String cid = clientId + ":" + clientSecret;
            String b64 = Base64.getEncoder().encodeToString(cid.getBytes(StandardCharsets.ISO_8859_1));
            headers.put("Authorization", "Basic " + b64);
            String postUrl;
            if (url.endsWith("/oauth/token")) {
                postUrl = url + "?grant_type=client_credentials";
            } else {
                postUrl = url + "/oauth/token" + "?grant_type=client_credentials";
            }

            String response = this.post(postUrl, body, headers);
            return this.extractToken(response);
        }

        String extractToken(String response) throws IOException {
            int index = response.indexOf("\"access_token\"");
            int tokenIndex = response.indexOf(34, index + 14) + 1;
            int lastTokenIndex = response.indexOf(34, tokenIndex);
            if (index > 0 && lastTokenIndex > tokenIndex) {
                return response.substring(tokenIndex, lastTokenIndex);
            } else {
                throw new IOException("Unable to extract access_token from response: " + response);
            }
        }

        private String post(String url, String content, Map<String, String> additionalHeaders) throws IOException {
            HttpURLConnection con = this.openConnection(url);
            con.setRequestMethod("POST");
            additionalHeaders.forEach(con::setRequestProperty);
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            Throwable var6 = null;

            String var11;
            try {
                WritableByteChannel outChannel = Channels.newChannel(os);
                Throwable var8 = null;

                try {
                    ByteBuffer outBuffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
                    int wrote = outChannel.write(outBuffer);
                    if (wrote < 0) {
                        throw new IOException("No data written");
                    }

                    var11 = this.handleResponse(con);
                } catch (Throwable var34) {
                    var8 = var34;
                    throw var34;
                } finally {
                    if (outChannel != null) {
                        if (var8 != null) {
                            try {
                                outChannel.close();
                            } catch (Throwable var33) {
                                var8.addSuppressed(var33);
                            }
                        } else {
                            outChannel.close();
                        }
                    }

                }
            } catch (Throwable var36) {
                var6 = var36;
                throw var36;
            } finally {
                if (os != null) {
                    if (var6 != null) {
                        try {
                            os.close();
                        } catch (Throwable var32) {
                            var6.addSuppressed(var32);
                        }
                    } else {
                        os.close();
                    }
                }

            }

            return var11;
        }

        private HttpURLConnection openConnection(String url) throws IOException {
            URL urly = new URL(url);
            Proxy proxy = this.getProxy();
            HttpURLConnection con;
            if (proxy == null) {
                con = (HttpURLConnection) urly.openConnection();
            } else {
                con = (HttpURLConnection) urly.openConnection(proxy);
            }

            return con;
        }

        private Proxy getProxy() {
            String host = System.getProperty("http.proxyHost");
            String portParam;
            int port;
            InetSocketAddress addr;
            if (host != null) {
                portParam = System.getProperty("http.proxyPort");
                port = Integer.parseInt(portParam);
                addr = new InetSocketAddress(host, port);
                return new Proxy(Proxy.Type.HTTP, addr);
            } else {
                host = System.getProperty("https.proxyHost");
                if (host != null) {
                    portParam = System.getProperty("https.proxyPort");
                    port = Integer.parseInt(portParam);
                    addr = new InetSocketAddress(host, port);
                    return new Proxy(Proxy.Type.HTTP, addr);
                } else {
                    return null;
                }
            }
        }

        private String handleResponse(HttpURLConnection con) throws IOException {
            int responseCode = con.getResponseCode();
            if (!this.is2xx(responseCode)) {
                String message = String
                        .format("Unable to Fetch a token: got a none 2xx response code '%s' from OAuth-Endpoint '%s'.", responseCode,
                                con.getURL());
                //LOG.error(message);
                throw new IOException(message);
            } else {
                return this.readResponseBody(con);
            }
        }

        private String readResponseBody(HttpURLConnection con) throws IOException {
            Charset responseCharset = this.getCharset(con);
            InputStream is = con.getInputStream();
            Throwable var4 = null;

            try {
                ReadableByteChannel inChannel = Channels.newChannel(is);
                Throwable var6 = null;

                try {
                    ByteBuffer inBuffer = ByteBuffer.allocate(65536);
                    byte[] tmp = new byte[65536];
                    StringBuilder response = new StringBuilder();
                    int maxRead = 655360;

                    for (int read = inChannel.read(inBuffer); read > 0 && maxRead > 0; read = inChannel.read(inBuffer)) {
                        maxRead -= read;
                        inBuffer.flip();
                        inBuffer.get(tmp, 0, read);
                        response.append(new String(tmp, 0, read, responseCharset));
                        inBuffer.clear();
                    }

                    if (maxRead <= 0) {
                        throw new IOException("Buffer overflow for response.");
                    } else {
                        String var12 = response.toString();
                        return var12;
                    }
                } catch (Throwable var35) {
                    var6 = var35;
                    throw var35;
                } finally {
                    if (inChannel != null) {
                        if (var6 != null) {
                            try {
                                inChannel.close();
                            } catch (Throwable var34) {
                                var6.addSuppressed(var34);
                            }
                        } else {
                            inChannel.close();
                        }
                    }

                }
            } catch (Throwable var37) {
                var4 = var37;
                throw var37;
            } finally {
                if (is != null) {
                    if (var4 != null) {
                        try {
                            is.close();
                        } catch (Throwable var33) {
                            var4.addSuppressed(var33);
                        }
                    } else {
                        is.close();
                    }
                }

            }
        }

        private Charset getCharset(HttpURLConnection connection) {
            String contentTypeHeader = connection.getHeaderField("Content-Type");
            if (contentTypeHeader != null) {
                String contentTypeHeaderLc = contentTypeHeader.toLowerCase(Locale.US);
                if (contentTypeHeaderLc.contains("utf-8")) {
                    return StandardCharsets.UTF_8;
                }

                if (contentTypeHeaderLc.contains("iso-8859-1")) {
                    return StandardCharsets.ISO_8859_1;
                }
            }

            return StandardCharsets.US_ASCII;
        }

        private boolean is2xx(int responseCode) {
            return responseCode >= 200 && responseCode < 300;
        }

        public static final class AuthSettings {
            private String oAuthGrantType;
            private String oAuthClientId;
            private String oAuthClientSecret;
            private String oAuthEndpoint;

            private AuthSettings() {
            }

            static OAuthHandler.AuthSettings createClientCredentialsGrant(String tokenEndpoint, String clientId, String clientSecret) {
                OAuthHandler.AuthSettings auth = new OAuthHandler.AuthSettings();
                auth.oAuthClientId = clientId;
                auth.oAuthClientSecret = clientSecret;
                auth.oAuthEndpoint = tokenEndpoint;
                auth.oAuthGrantType = "client_credentials";
                return auth;
            }

            String getoAuthGrantType() {
                return this.oAuthGrantType;
            }

            String getoAuthClientId() {
                return this.oAuthClientId;
            }

            String getoAuthClientSecret() {
                return this.oAuthClientSecret;
            }

            String getoAuthEndpoint() {
                return this.oAuthEndpoint;
            }
        }
    }
}
