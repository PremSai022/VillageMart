package com.villagemart.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.villagemart.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Thin wrapper over Firebase Admin SDK. Disabled (no-op) when FIREBASE_CREDENTIALS_BASE64 is not configured. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseMessagingGateway {

    private static final String APP_NAME = "villagemart";

    private final AppProperties properties;
    private FirebaseMessaging messaging;

    public record SendResult(int successCount, List<String> invalidTokens, String error) {
    }

    @PostConstruct
    void init() {
        String encoded = properties.firebase() == null ? null : properties.firebase().credentialsBase64();
        if (encoded == null || encoded.isBlank()) {
            log.info("Firebase credentials not configured; push notifications disabled (alerts remain in-app)");
            return;
        }
        try {
            byte[] json = Base64.getDecoder().decode(encoded.trim());
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(json)))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().stream().filter(a -> a.getName().equals(APP_NAME)).findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
            messaging = FirebaseMessaging.getInstance(app);
            log.info("Firebase Cloud Messaging initialised");
        } catch (IOException | IllegalArgumentException e) {
            log.error("Invalid FIREBASE_CREDENTIALS_BASE64; push notifications disabled: {}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return messaging != null;
    }

    public SendResult send(List<String> tokens, String title, String body, Map<String, String> data) {
        if (!isEnabled()) {
            return new SendResult(0, List.of(), "Firebase not configured");
        }
        if (tokens.isEmpty()) {
            return new SendResult(0, List.of(), "No registered devices");
        }
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(com.google.firebase.messaging.Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                .build();
        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            List<String> invalid = new ArrayList<>();
            String lastError = null;
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (!r.isSuccessful() && r.getException() != null) {
                    MessagingErrorCode code = r.getException().getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        invalid.add(tokens.get(i));
                    } else {
                        lastError = r.getException().getMessage();
                    }
                }
            }
            return new SendResult(response.getSuccessCount(), invalid, lastError);
        } catch (FirebaseMessagingException e) {
            return new SendResult(0, List.of(), e.getMessage());
        }
    }
}
