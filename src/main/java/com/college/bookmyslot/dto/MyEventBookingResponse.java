package com.college.bookmyslot.dto;
import lombok.Data;

@Data
public class MyEventBookingResponse {

    private Long bookingId;

    private String eventTitle;
    private String eventDate;
    private String eventTime;
    private String venue;

    private String bookingDate;
    private String bookingStatus;
    private String refundStatus;
    private String ticketId;
    private String qrCodeUrl;

}
