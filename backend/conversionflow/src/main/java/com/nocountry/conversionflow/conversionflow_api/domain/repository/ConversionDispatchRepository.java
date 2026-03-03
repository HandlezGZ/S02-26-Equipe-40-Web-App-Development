package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.DispatchStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.projection.ProviderDispatchStatusCountProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversionDispatchRepository
        extends JpaRepository<ConversionDispatch, Long> {

    List<ConversionDispatch> findByStatusIn(List<DispatchStatus> statuses);

    long countByStatus(DispatchStatus status);

    List<ConversionDispatch> findByStatusOrderByLastAttemptAtDesc(DispatchStatus status, Pageable pageable);

    @Query("select coalesce(avg(cd.attemptCount), 0) from ConversionDispatch cd")
    Double findAverageAttemptCount();

    @Query(value = """
            select provider as provider, status as status, count(*) as total
            from conversion_dispatch
            group by provider, status
            """, nativeQuery = true)
    List<ProviderDispatchStatusCountProjection> findProviderDispatchStatusCounts();
}
