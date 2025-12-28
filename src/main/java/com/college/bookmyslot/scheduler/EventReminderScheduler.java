package com.college.bookmyslot.scheduler;

import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.repository.EventBookingRepository;
import com.college.bookmyslot.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class EventReminderScheduler {

    private final EventBookingRepository bookingRepository;
    private final EmailService emailService;

    public EventReminderScheduler(
            EventBookingRepository bookingRepository,
            EmailService emailService
    ) {
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 300000)
    public void sendEventReminders() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        LocalDate date = reminderTime.toLocalDate();
        LocalTime start = reminderTime.minusMinutes(1).toLocalTime();
        LocalTime end = reminderTime.plusMinutes(1).toLocalTime();

        List<EventBooking> bookings =
                bookingRepository.findUpcomingBookingsForReminder(date, start, end);

        for (EventBooking booking : bookings) {

            emailService.sendEventReminderEmail(
                    booking.getStudent().getEmail(),
                    booking.getStudent().getName(),
                    booking.getEvent().getTitle(),
                    booking.getEvent().getVenue(),
                    booking.getEvent().getStartTime().toString()
            );

            booking.setReminderSent(true);
            bookingRepository.save(booking);
        }
    }
}
