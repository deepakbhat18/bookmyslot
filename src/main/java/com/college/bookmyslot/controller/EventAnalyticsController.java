package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.EventAnalyticsDto;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.model.Payment;
import com.college.bookmyslot.repository.EventBookingRepository;
import com.college.bookmyslot.repository.EventRepository;
import com.college.bookmyslot.repository.PaymentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/event-analytics")
//@CrossOrigin(origins = "*")
public class EventAnalyticsController {

    private final EventRepository eventRepository;
    private final EventBookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public EventAnalyticsController(EventRepository eventRepository,
                                    EventBookingRepository bookingRepository,
                                    PaymentRepository paymentRepository) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }
    @GetMapping
    public List<EventAnalyticsDto> getAllEventAnalytics() {

        List<Event> events = eventRepository.findAll();
        List<EventAnalyticsDto> result = new ArrayList<>();

        for (Event event : events) {

            List<EventBooking> bookings =
                    bookingRepository.findByEvent(event);

            long checkedInCount = bookings.stream()
                    .filter(EventBooking::isCheckedIn)
                    .count();
            double revenue = paymentRepository.findByEvent(event)
                    .stream()
                    .filter(p -> p.getStatus() == Payment.Status.SUCCESS)
                    .mapToDouble(Payment::getAmount)
                    .sum();
            EventAnalyticsDto dto = new EventAnalyticsDto();
            dto.setEventId(event.getId());
            dto.setEventTitle(event.getTitle());
            dto.setClubName(event.getClub().getName());
            dto.setTotalSeats(event.getTotalSlots());
            dto.setBookedSeats(event.getBookedSlots());
            dto.setCheckedInCount(checkedInCount);
            dto.setRevenue(revenue);
            dto.setStatus(event.getStatus().name());

            dto.setAttendancePercentage(
                    event.getBookedSlots() == 0 ? 0 :
                            (checkedInCount * 100.0) / event.getBookedSlots()
            );
            result.add(dto);
        }
        return result;
    }
    @GetMapping("/{eventId}")
    public EventAnalyticsDto getEventAnalytics(@PathVariable Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        List<EventBooking> bookings =
                bookingRepository.findByEvent(event);

        long checkedInCount = bookings.stream()
                .filter(EventBooking::isCheckedIn)
                .count();

        double revenue = paymentRepository.findByEvent(event)
                .stream()
                .filter(p -> p.getStatus() == Payment.Status.SUCCESS)
                .mapToDouble(Payment::getAmount)
                .sum();

        EventAnalyticsDto dto = new EventAnalyticsDto();
        dto.setEventId(event.getId());
        dto.setEventTitle(event.getTitle());
        dto.setClubName(event.getClub().getName());
        dto.setTotalSeats(event.getTotalSlots());
        dto.setBookedSeats(event.getBookedSlots());
        dto.setCheckedInCount(checkedInCount);
        dto.setRevenue(revenue);
        dto.setStatus(event.getStatus().name());

        dto.setAttendancePercentage(
                event.getBookedSlots() == 0 ? 0 :
                        (checkedInCount * 100.0) / event.getBookedSlots()
        );
        return dto;
    }

}