package io.sekretess.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

class TokenProvider {

    private final String idpUrl = System.getenv("IDENTITY_PROVIDER_URL");
    private final String clientId = "business_client";
    private final String clientCert = System.getenv("USER_CERTIFICATE_PATH");
    private final String clientKey = System.getenv("USER_CERTIFICATE_KEY");
    private final String clientKeyPassword = System.getenv("USER_CERTIFICATE_PASSWORD");
    private final HttpClient httpClient;
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(22, TimeUnit.MINUTES)
            .build();

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    TokenProvider() {
        this.httpClient = HttpClient.newBuilder().sslContext(createSslContext()).build();
    }

    private SSLContext createSslContext() {
        try {
            X509Certificate certificate;
            try (FileInputStream fis = new FileInputStream(clientCert)) {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                certificate = (X509Certificate) factory.generateCertificate(fis);
            }

            PrivateKey privateKey;
            try (PEMParser pemParser = new PEMParser(new FileReader(clientKey))) {
                Object object = pemParser.readObject();
                if (object instanceof PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo) {
                    InputDecryptorProvider decryptorProvider =
                            new JceOpenSSLPKCS8DecryptorProviderBuilder()
                                    .build(clientKeyPassword.toCharArray());
                    privateKey = new JcaPEMKeyConverter().setProvider("BC")
                            .getPrivateKey(encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider));

                } else {
                    privateKey = new JcaPEMKeyConverter().setProvider("BC").getPrivateKey((PrivateKeyInfo) object);
                }
            }

            char[] keyStorePassword = io.sekretess.util.PasswordGenerator.generatePassword();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setKeyEntry("client", privateKey, keyStorePassword, new java.security.cert.Certificate[]{certificate});

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, keyStorePassword);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSLContext", e);
        }
    }

    public String getUserName() {
        String token = cache.get(clientId, k -> getToken());
        if (token == null) {
            throw new RuntimeException("Failed to get token!");
        }
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getClaim("preferred_username").asString();
    }


    private String getToken() {
        String body = "client_id=" + clientId + "&grant_type=password";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(idpUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get token: " + response.body());
        }
        Gson gson = new Gson();
        return gson.fromJson(response.body(), TokenObject.class).getAccess_token();
    }

    public String fetchToken() {
        String token = cache.get(clientId, k -> getToken());
        if (token == null) {
            throw new RuntimeException("Failed to get token!");
        }
        DecodedJWT jwt = JWT.decode(token);
        Date expiration = jwt.getExpiresAt();
        if (expiration.before(new Date())) {
            cache.invalidate(clientId);
            token = getToken();
            cache.put(clientId, token);

        }
        return token;
    }

    private static class TokenObject {
        private String access_token;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }
    }
}
