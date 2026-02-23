package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.controller.dto.CheckoutRequest;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.CheckoutResponse;
import com.nocountry.conversionflow.conversionflow_api.service.stripe.StripeWebhookService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final StripeWebhookService stripeWebhookService;

    public CheckoutController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> createCheckout(@Valid @RequestBody CheckoutRequest request)
            throws StripeException {

        log.info("Creating checkout. leadId={}, plan={}", request.getLeadId(), request.getPlan());

        String url = stripeWebhookService.createCheckoutSession(
                request.getLeadId(),
                request.getPlan(),
                request.getGclid(),
                request.getFbclid(),
                request.getFbp(),
                request.getFbc()
        );

        return ResponseEntity.ok(new CheckoutResponse(url));
    }
}