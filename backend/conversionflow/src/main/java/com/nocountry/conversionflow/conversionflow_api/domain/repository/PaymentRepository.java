package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByStripeEventId(String stripeEventId);

    boolean existsByTransactionId(String transactionId);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);
}