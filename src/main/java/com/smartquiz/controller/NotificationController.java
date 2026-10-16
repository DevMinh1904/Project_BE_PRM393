package com.smartquiz.controller;

import com.smartquiz.dto.ApiResponse;
import com.smartquiz.dto.CreateNotificationRequest;
import com.smartquiz.dto.NotificationDto;
import com.smartquiz.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Notifications", description = "Endpoints for managing user study reminders and system alerts")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    @Operation(summary = "Fetch user study reminders and alerts", description = "Retrieves all notifications and study alerts for a specific user.")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotificationsByUserId(@PathVariable String userId) {
        List<NotificationDto> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(notifications, "Notifications retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create custom notification / study alert", description = "Dispatches a new study alert or reminder to a user.")
    public ResponseEntity<ApiResponse<NotificationDto>> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationDto notification = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(notification, "Notification created successfully"));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read.")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable String id) {
        NotificationDto notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok(notification, "Notification marked as read"));
    }
}
