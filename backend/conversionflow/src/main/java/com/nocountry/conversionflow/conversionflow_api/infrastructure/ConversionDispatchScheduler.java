package com.nocountry.conversionflow.conversionflow_api.infrastructure;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.DispatchStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.ConversionDispatchRepository;
import com.nocountry.conversionflow.conversionflow_api.service.google.GoogleConversionsService;
import com.nocountry.conversionflow.conversionflow_api.service.meta.MetaConversionsService;
import com.nocountry.conversionflow.conversionflow_api.service.pipedrive.PipedriveService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversionDispatchScheduler {

    private static final int MAX_ATTEMPTS = 5;

    private final ConversionDispatchRepository repository;
    private final GoogleConversionsService googleService;
    private final MetaConversionsService metaService;
    private final PipedriveService pipedriveService;

    public ConversionDispatchScheduler(
            ConversionDispatchRepository repository,
            GoogleConversionsService googleService,
            MetaConversionsService metaService,
            PipedriveService pipedriveService
    ) {
        this.repository = repository;
        this.googleService = googleService;
        this.metaService = metaService;
        this.pipedriveService = pipedriveService;
    }

    @Scheduled(fixedDelay = 60000)
    public void processPendingDispatches() {

        List<ConversionDispatch> dispatches =
                repository.findByStatusIn(List.of(DispatchStatus.PENDING, DispatchStatus.FAILED));

        for (ConversionDispatch dispatch : dispatches) {

            if (!dispatch.canRetry(MAX_ATTEMPTS)) {
                continue;
            }

            try {
                switch (dispatch.getProvider()) {
                    case GOOGLE -> googleService.sendConversionFromPayload(dispatch.getPayload());
                    case META -> metaService.sendConversionFromPayload(dispatch.getPayload());
                    case PIPEDRIVE -> pipedriveService.syncFromPayload(dispatch.getPayload());
                }

                dispatch.markSuccess();

            } catch (Exception e) {
                dispatch.markFailure(e.getMessage());
            }

            repository.save(dispatch);
        }
    }
}