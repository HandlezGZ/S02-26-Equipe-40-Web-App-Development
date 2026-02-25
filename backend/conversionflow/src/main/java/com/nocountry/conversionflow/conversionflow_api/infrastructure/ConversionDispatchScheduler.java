package com.nocountry.conversionflow.conversionflow_api.infrastructure;

import com.nocountry.conversionflow.conversionflow_api.application.usecase.ProcessDispatchQueueUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConversionDispatchScheduler {

    private final ProcessDispatchQueueUseCase processDispatchQueueUseCase;

    public ConversionDispatchScheduler(
            ProcessDispatchQueueUseCase processDispatchQueueUseCase
    ) {
        this.processDispatchQueueUseCase = processDispatchQueueUseCase;
    }

    @Scheduled(fixedDelay = 60000)
    public void processPendingDispatches() {
        processDispatchQueueUseCase.execute();
    }
}
