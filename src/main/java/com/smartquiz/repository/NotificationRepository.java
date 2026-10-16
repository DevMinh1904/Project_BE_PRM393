package com.smartquiz.repository;

import com.smartquiz.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserId(String userId);

    List<Notification> findByUserIdOrderByTimestampDesc(String userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByTimestampDesc(String userId);
}
