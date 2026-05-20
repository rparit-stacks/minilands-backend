package com.minilands.backend.service.payment;

import com.minilands.backend.config.RazorpayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class RazorpaySignatureVerifier {

    private final RazorpayProperties razorpayProperties;

    public RazorpaySignatureVerifier(RazorpayProperties razorpayProperties) {
        this.razorpayProperties = razorpayProperties;
    }

    /** Checkout success handler: HMAC(order_id|payment_id). */
    public boolean isPaymentSignatureValid(String orderId, String paymentId, String signature) {
        String secret = razorpayProperties.getKeySecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Razorpay key secret is not configured");
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expected = hmacSha256Hex(orderId + "|" + paymentId, secret);
        return constantTimeHexEquals(expected, signature);
    }

    /** Webhook: HMAC(raw request body). */
    public boolean isWebhookSignatureValid(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String secret = razorpayProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret is not configured");
        }
        String expected = hmacSha256Hex(rawBody, secret);
        return constantTimeHexEquals(expected, signatureHeader);
    }

    /** Compares two lowercase hex strings in constant time (mitigates timing leaks). */
    private static boolean constantTimeHexEquals(String expectedHex, String actualHex) {
        try {
            byte[] a = HexFormat.of().parseHex(expectedHex.toLowerCase());
            byte[] b = HexFormat.of().parseHex(actualHex.trim().toLowerCase());
            return MessageDigest.isEqual(a, b);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute Razorpay signature", ex);
        }
    }
}
