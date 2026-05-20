package com.minilands.backend.controller;

import com.minilands.backend.service.payment.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Step 4 in deposit flow: Razorpay webhook → confirm deposit + wallet commit / rollback.
 */
@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    public RazorpayWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            paymentService.handleWebhook(rawBody, signature);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
