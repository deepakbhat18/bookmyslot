package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.*;
import com.college.bookmyslot.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
//@CrossOrigin("*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ApiResponse<PaymentResponse> createPayment(
            @RequestBody PaymentCreateRequest request
    ) {
        return new ApiResponse<>(
                true,
                "Payment initiated",
                paymentService.createRazorpayOrder(request)
        );
    }
    @PostMapping("/verify")
    public ApiResponse<EventBookingResponse> verifyPayment(
            @RequestBody PaymentVerifyRequest request
    ) {
        return new ApiResponse<>(
                true,
                "Payment verified & event booked",
                paymentService.verifyPayment(request)
        );
    }

}
