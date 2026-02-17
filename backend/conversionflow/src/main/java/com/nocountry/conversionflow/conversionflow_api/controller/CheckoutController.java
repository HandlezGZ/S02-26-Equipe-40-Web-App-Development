package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final StripeService stripeService;

    public CheckoutController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping
    public ResponseEntity<String> createCheckout(@RequestBody Map<String, Object> request) {

        Long leadId = Long.valueOf(request.get("leadId").toString());
        String plan = request.get("plan").toString();

        String checkoutUrl = stripeService.createCheckoutSession(leadId, plan);

        return ResponseEntity.ok(checkoutUrl);
    }
}
