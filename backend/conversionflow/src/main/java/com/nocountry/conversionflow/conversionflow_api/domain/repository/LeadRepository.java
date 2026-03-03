package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}