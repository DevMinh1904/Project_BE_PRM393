package com.smartquiz.service;

import com.smartquiz.dto.CreateNotificationRequest;
import com.smartquiz.dto.NotificationDto;

import java.util.List;

public interface NotificationService {

    List<NotificationDto> getUserNotifications(Long userId);

    NotificationDto createNotification(CreateNotificationRequest request);

    NotificationDto markAsRead(Long notificationId);
}
