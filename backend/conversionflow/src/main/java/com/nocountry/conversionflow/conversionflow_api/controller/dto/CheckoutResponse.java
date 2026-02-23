package com.nocountry.conversionflow.conversionflow_api.controller.dto;

public class CheckoutResponse {

    private final String checkoutUrl;

    public CheckoutResponse(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }
}