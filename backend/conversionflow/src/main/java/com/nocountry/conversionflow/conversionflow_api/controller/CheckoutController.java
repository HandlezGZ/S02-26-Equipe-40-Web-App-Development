package com.nocountry.conversionflow.conversionflow_api.controller;

import java.util.Map;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.CheckoutRequest;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.CheckoutResponse;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.LeadStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final StripeProperties stripeProperties;
    private final LeadRepository leadRepository;

    public CheckoutController(StripeProperties stripeProperties, LeadRepository leadRepository) {
        this.stripeProperties = stripeProperties;
        this.leadRepository = leadRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(@Valid @RequestBody CheckoutRequest request) throws Exception {

        log.info("==== INICIANDO CHECKOUT ====");
        log.info("PLAN recebido = {}", request.getPlan());
        log.info("MAPA DE PRICES carregado = {}", stripeProperties.getPrices());

        Lead lead = leadRepository.findById(request.getLeadId())
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado: " + request.getLeadId()));

        String priceId = resolvePriceId(request.getPlan(), stripeProperties.getPrices());

        log.info("PriceId resolvido para o plan '{}' = {}", request.getPlan(), priceId);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.getSuccessUrl())
                .setCancelUrl(stripeProperties.getCancelUrl())
                .setClientReferenceId(String.valueOf(lead.getId()))
                .putMetadata("leadId", String.valueOf(lead.getId()))
                .putMetadata("plan", request.getPlan())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                )
                .build();

        Session session = Session.create(params);

        log.info("Checkout criado com sucesso. SessionId={} URL={}", session.getId(), session.getUrl());

        lead.setExternalId(session.getId());
        lead.setStatus(LeadStatus.CHECKOUT_STARTED);

        leadRepository.save(lead);

        log.info("Lead atualizado para CHECKOUT_STARTED. leadId={}", lead.getId());
        log.info("==== FIM CHECKOUT ====");

        return ResponseEntity.ok(new CheckoutResponse(session.getUrl(), session.getId()));
    }

    private String resolvePriceId(String plan, Map<String, String> prices) {
        if (plan == null || plan.isBlank()) {
            throw new IllegalArgumentException("Plan inválido");
        }

        String priceId = prices.get(plan);

        if (priceId == null || priceId.isBlank()) {
            throw new IllegalArgumentException("Plan não mapeado para priceId: " + plan);
        }

        return priceId;
    }
}