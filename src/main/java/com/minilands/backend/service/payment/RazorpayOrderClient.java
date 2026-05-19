package com.minilands.backend.service.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minilands.backend.config.RazorpayProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RazorpayOrderClient {

    private final RazorpayProperties razorpayProperties;
    private final RestClient restClient;

    public RazorpayOrderClient(RazorpayProperties razorpayProperties, RestClient.Builder restClientBuilder) {
        this.razorpayProperties = razorpayProperties;
        this.restClient = restClientBuilder.baseUrl("https://api.razorpay.com").build();
    }

    public RazorpayOrder createOrder(long amountInPaise, String receipt, String userId) {
        requireConfigured();

        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("user_id", userId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountInPaise);
        body.put("currency", "INR");
        body.put("receipt", receipt);
        body.put("notes", notes);

        try {
            CreateOrderResponse response = restClient.post()
                    .uri("/v1/orders")
                    .headers(headers -> headers.setBasicAuth(
                            razorpayProperties.getKeyId(),
                            razorpayProperties.getKeySecret()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(CreateOrderResponse.class);

            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new IllegalStateException("Razorpay returned an invalid order response");
            }
            return new RazorpayOrder(response.id(), response.amount(), response.currency());
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Failed to create Razorpay order: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private void requireConfigured() {
        if (!razorpayProperties.isConfigured()) {
            throw new IllegalStateException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreateOrderResponse(
            String id,
            @JsonProperty("amount") Long amount,
            String currency
    ) {
    }

    public record RazorpayOrder(String id, Long amountInPaise, String currency) {
    }
}
