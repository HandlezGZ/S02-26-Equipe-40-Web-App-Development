package com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
}