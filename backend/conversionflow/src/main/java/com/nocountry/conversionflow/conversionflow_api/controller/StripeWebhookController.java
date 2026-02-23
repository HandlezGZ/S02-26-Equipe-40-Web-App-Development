package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.service.stripe.StripeWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        stripeWebhookService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("ok");
    }
}