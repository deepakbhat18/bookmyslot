package com.college.bookmyslot.controller;

import com.college.bookmyslot.model.Notification;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.NotificationRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                  UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }


    @PostMapping("/create/{userId}")
    public Notification createNotification(@PathVariable Long userId,
                                           @RequestBody Notification incoming) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(incoming.getTitle());
        n.setMessage(incoming.getMessage());

        return notificationRepository.save(n);
    }


    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }


    @PostMapping("/user/{userId}/mark-read")
    public Map<String, String> markAllRead(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification n : list) {
            n.setReadFlag(true);
        }
        notificationRepository.saveAll(list);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "All notifications marked as read");
        return resp;
    }


    @GetMapping("/user/{userId}/unread-count")
    public Map<String, Long> unreadCount(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long count = notificationRepository.countByUserAndReadFlagFalse(user);
        Map<String, Long> resp = new HashMap<>();
        resp.put("unread", count);
        return resp;
    }
}
