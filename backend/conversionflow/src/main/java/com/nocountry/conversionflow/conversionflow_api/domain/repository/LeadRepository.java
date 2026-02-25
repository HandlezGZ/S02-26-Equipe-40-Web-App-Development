package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.LeadStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.projection.DailyLeadCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

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

    long countByStatus(LeadStatus status);

    @Query("select count(l) from Lead l where l.createdAt >= :from")
    long countCreatedAtOrAfter(@Param("from") OffsetDateTime from);

    @Query("select count(l) from Lead l where l.convertedAt is not null and l.convertedAt >= :from")
    long countConvertedAtOrAfter(@Param("from") OffsetDateTime from);

    @Query("select count(l) from Lead l where l.gclid is not null and l.gclid <> ''")
    long countWithGclid();

    @Query("select count(l) from Lead l where l.fbclid is not null and l.fbclid <> ''")
    long countWithFbclid();

    @Query("select count(l) from Lead l where l.fbp is not null and l.fbp <> ''")
    long countWithFbp();

    @Query("select count(l) from Lead l where l.fbc is not null and l.fbc <> ''")
    long countWithFbc();

    @Query(value = """
            select date(created_at) as day, count(*) as leads
            from leads
            where created_at >= :from
            group by date(created_at)
            order by day
            """, nativeQuery = true)
    List<DailyLeadCountProjection> findDailyLeadCounts(@Param("from") OffsetDateTime from);
}
