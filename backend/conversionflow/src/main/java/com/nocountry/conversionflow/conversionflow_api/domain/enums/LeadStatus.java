package com.nocountry.conversionflow.conversionflow_api.domain.enums;

public enum LeadStatus {

    // Entrada
    NEW,                    // Lead recém criado

    // Pré-venda
    CONTACTED,              // Contato realizado
    QUALIFIED,              // Lead qualificado

    // Intenção de compra
    CHECKOUT_STARTED,       // Iniciou checkout Stripe
    PROPOSAL_SENT,          // Proposta comercial enviada
    NEGOTIATION,            // Em negociação

    // Final
    WON,                    // Pagamento confirmado / Negócio fechado
    LOST                    // Não converteu
}