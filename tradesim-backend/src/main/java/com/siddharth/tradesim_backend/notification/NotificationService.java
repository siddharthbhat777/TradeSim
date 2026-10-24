package com.siddharth.tradesim_backend.notification;

import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.notification.enums.NotificationType;
import com.siddharth.tradesim_backend.notification.model.Notification;
import com.siddharth.tradesim_backend.notification.model.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthRepository authRepository;

    @Transactional
    public void notifyAdmins(String message, String referenceId, NotificationType type) {
        List<User> admins = authRepository.findByRole(Role.ADMIN);

        for (User admin : admins) {
            Notification notif = Notification.builder()
                    .userId(admin.getId())
                    .message(message)
                    .referenceId(referenceId)
                    .type(type)
                    .build();

            Notification saved = notificationRepository.save(notif);

            NotificationResponse dto = new NotificationResponse(saved.getId(), saved.getMessage(), saved.getReferenceId(), saved.getType(), saved.isRead(), saved.getCreatedAt());

            messagingTemplate.convertAndSendToUser(admin.getUsername(), "/queue/admin-alerts", dto);
        }
    }
}