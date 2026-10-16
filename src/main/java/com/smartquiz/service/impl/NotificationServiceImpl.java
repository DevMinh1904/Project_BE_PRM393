package com.smartquiz.service.impl;

import com.smartquiz.dto.CreateNotificationRequest;
import com.smartquiz.dto.NotificationDto;
import com.smartquiz.entity.Notification;
import com.smartquiz.entity.User;
import com.smartquiz.exception.ResourceNotFoundException;
import com.smartquiz.repository.NotificationRepository;
import com.smartquiz.repository.UserRepository;
import com.smartquiz.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        log.info("Fetching notifications for user ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return notificationRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationDto createNotification(CreateNotificationRequest request) {
        log.info("Creating notification for user ID: {}", request.getUserId());
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        Notification notification = Notification.builder()
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .user(user)
                .build();

        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        notification.setIsRead(true);
        Notification updated = notificationRepository.save(notification);
        return mapToDto(updated);
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .timestamp(n.getTimestamp())
                .isRead(n.getIsRead())
                .userId(n.getUser() != null ? n.getUser().getId() : null)
                .build();
    }
}
