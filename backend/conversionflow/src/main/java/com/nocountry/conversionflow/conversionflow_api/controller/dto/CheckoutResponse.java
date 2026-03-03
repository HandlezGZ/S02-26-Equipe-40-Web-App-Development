package com.nocountry.conversionflow.conversionflow_api.controller.dto;

public class CheckoutResponse {

    private String checkoutUrl;
    private String sessionId;

    public CheckoutResponse(String checkoutUrl, String sessionId) {
        this.checkoutUrl = checkoutUrl;
        this.sessionId = sessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getSessionId() {
        return sessionId;
    }
}