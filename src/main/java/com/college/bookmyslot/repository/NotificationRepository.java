package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.Notification;
import com.college.bookmyslot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadFlagFalse(User user);
}
