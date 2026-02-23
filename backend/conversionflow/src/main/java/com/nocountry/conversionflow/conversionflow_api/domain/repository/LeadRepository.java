package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    /**
     * Buscar lead pelo ID externo (ex: Webflow, frontend)
     */
    Optional<Lead> findByExternalId(String externalId);

    /**
     * Buscar lead pelo e-mail
     */
    Optional<Lead> findByEmail(String email);

    /**
     * Verificar se já existe lead com esse externalId
     */
    boolean existsByExternalId(String externalId);
}
