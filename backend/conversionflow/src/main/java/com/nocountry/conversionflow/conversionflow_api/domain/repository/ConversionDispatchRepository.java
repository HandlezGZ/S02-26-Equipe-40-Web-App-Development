package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversionDispatchRepository
        extends JpaRepository<ConversionDispatch, Long> {

    List<ConversionDispatch> findByStatusIn(List<DispatchStatus> statuses);
}