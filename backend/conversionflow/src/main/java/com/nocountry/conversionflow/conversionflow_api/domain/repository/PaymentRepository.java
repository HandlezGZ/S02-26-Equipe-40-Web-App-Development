package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.projection.DailyPaymentStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByStripeEventId(String stripeEventId);

    boolean existsByTransactionId(String transactionId);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    long countByStatus(PaymentStatus status);

    @Query("select count(p) from Payment p where p.status = :status and p.createdAt >= :from")
    long countByStatusAndCreatedAtOrAfter(@Param("status") PaymentStatus status, @Param("from") OffsetDateTime from);

    @Query("select coalesce(sum(p.amountCents), 0) from Payment p where p.status = :status")
    Long sumAmountCentsByStatus(@Param("status") PaymentStatus status);

    @Query("select coalesce(sum(p.amountCents), 0) from Payment p where p.status = :status and p.createdAt >= :from")
    Long sumAmountCentsByStatusAndCreatedAtOrAfter(@Param("status") PaymentStatus status, @Param("from") OffsetDateTime from);

    @Query(value = """
            select
                date(created_at) as day,
                count(*) as paidPayments,
                coalesce(sum(amount), 0) as revenueCents
            from payments
            where status = 'PAID'
              and created_at >= :from
            group by date(created_at)
            order by day
            """, nativeQuery = true)
    List<DailyPaymentStatsProjection> findDailyPaidPaymentStats(@Param("from") OffsetDateTime from);
}
