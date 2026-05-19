package com.minilands.backend.entity.enums;

/**
 * Payment outcome reported by frontend after Razorpay Checkout completes.
 */
public enum PaymentReportStatus {
    SUCCESS,
    FAILED,
    CANCELLED
}
