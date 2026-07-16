package com.laserxprts.falcon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import com.laserxprts.falcon.model.Notification;
import com.laserxprts.falcon.model.User;
import com.laserxprts.falcon.repository.NotificationRepository;
import com.laserxprts.falcon.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void sendNotificationToUser(User user, String title, String body, String imageUrl, Map<String, String> data) {
        // Save to DB first
        Notification notification = Notification.builder()
                .userId(user.getId())
                .title(title)
                .body(body)
                .imageUrl(imageUrl)
                .data(data)
                .read(false)
                .build();
        notificationRepository.save(notification);

        if (user.getFcmTokens() == null || user.getFcmTokens().isEmpty()) {
            return;
        }

        List<String> tokens = new ArrayList<>(user.getFcmTokens());
        sendMulticast(tokens, title, body, imageUrl, data, user);
    }

    public void broadcastNotification(String title, String body, String imageUrl, Map<String, String> data) {
        // Save broadcast notification to DB
        Notification notification = Notification.builder()
                .userId(null) // Broadcast
                .title(title)
                .body(body)
                .imageUrl(imageUrl)
                .data(data)
                .read(false)
                .build();
        notificationRepository.save(notification);

        // Fetch all users with tokens directly from DB
        List<User> allUsers = userRepository.findUsersWithFcmTokens();

        List<String> allTokens = new ArrayList<>();
        for (User user : allUsers) {
            allTokens.addAll(user.getFcmTokens());
        }

        if (allTokens.isEmpty()) return;

        // Firebase multicast has a limit of 500 messages per batch
        int batchSize = 500;
        for (int i = 0; i < allTokens.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, allTokens.size());
            List<String> chunk = allTokens.subList(i, endIndex);
            sendMulticast(chunk, title, body, imageUrl, data, null);
        }
    }

    private void sendMulticast(List<String> tokens, String title, String body, String imageUrl, Map<String, String> data, User user) {
        try {
            com.google.firebase.messaging.Notification fcmNotification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .setImage(imageUrl)
                    .build();

            List<Message> messages = tokens.stream().map(token -> {
                Message.Builder builder = Message.builder()
                        .setToken(token)
                        .setNotification(fcmNotification)
                        .setAndroidConfig(
                            com.google.firebase.messaging.AndroidConfig.builder()
                                .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                                .setNotification(com.google.firebase.messaging.AndroidNotification.builder()
                                    .setChannelId("high_importance_channel")
                                    .setSound("default")
                                    .build())
                                .build()
                        );
                if (data != null) {
                    builder.putAllData(data);
                }
                return builder.build();
            }).collect(Collectors.toList());

            log.info("FCM: Sending notification to {} token(s). Title: '{}'", tokens.size(), title);

            // sendEach is the recommended replacement for the deprecated sendAll
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);

            log.info("FCM: Sent {} / {} messages successfully.", response.getSuccessCount(), messages.size());

            // Clean up invalid tokens
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        String errorCode = responses.get(i).getException().getMessagingErrorCode() != null
                                ? responses.get(i).getException().getMessagingErrorCode().name()
                                : "UNKNOWN";
                        log.warn("FCM: Token failed [{}]: {} — {}", i, tokens.get(i).substring(0, Math.min(20, tokens.get(i).length())), errorCode);
                        if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                            failedTokens.add(tokens.get(i));
                        }
                    }
                }
                
                if (!failedTokens.isEmpty()) {
                    log.info("FCM: Removing {} stale token(s).", failedTokens.size());
                    if (user != null) {
                        user.getFcmTokens().removeAll(failedTokens);
                        userRepository.save(user);
                    } else {
                        // Broadcast: clean up across only affected users
                        List<User> affectedUsers = userRepository.findUsersByFcmTokensIn(failedTokens);
                        for (User u : affectedUsers) {
                            if (u.getFcmTokens() != null && u.getFcmTokens().removeAll(failedTokens)) {
                                userRepository.save(u);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("FCM: Error sending notification batch", e);
        }
    }
}
