package com.nocountry.authservice.repository.outbox;

import com.nocountry.authservice.domain.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
