package com.nocountry.conversionflow.conversionflow_api.domain.enums;

public enum LeadStatus {

    NEW,                // Lead acabou de entrar
    CONTACTED,          // Time já entrou em contato
    QUALIFIED,          // Lead qualificado
    PROPOSAL_SENT,      // Proposta enviada
    NEGOTIATION,        // Em negociação
    WON,                // Fechou negócio
    LOST                // Não converteu

}
