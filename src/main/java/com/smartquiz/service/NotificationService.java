package com.smartquiz.service;

import com.smartquiz.dto.CreateNotificationRequest;
import com.smartquiz.dto.NotificationDto;

import java.util.List;

public interface NotificationService {

    List<NotificationDto> getUserNotifications(String userId);

    NotificationDto createNotification(CreateNotificationRequest request);

    NotificationDto markAsRead(String notificationId);
}
