package com.nocountry.authservice.integration.conversionflow;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ConversionFlowLeadClient {

    private static final Logger log = LoggerFactory.getLogger(ConversionFlowLeadClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;
    private final int circuitFailureThreshold;
    private final long circuitOpenMs;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong retryCount = new AtomicLong(0);
    private final AtomicLong circuitOpenCount = new AtomicLong(0);
    private volatile Instant circuitOpenedAt;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter retryCounter;
    private final Counter circuitOpenCounter;
    private final Timer requestTimer;

    public ConversionFlowLeadClient(
            RestClient.Builder restClientBuilder,
            MeterRegistry meterRegistry,
            @Value("${app.conversionflow.base-url}") String baseUrl,
            @Value("${app.conversionflow.api-key:}") String apiKey,
            @Value("${app.conversionflow.timeout-ms:2000}") int timeoutMs,
            @Value("${app.conversionflow.retry-max-attempts:3}") int retryMaxAttempts,
            @Value("${app.conversionflow.retry-backoff-ms:200}") long retryBackoffMs,
            @Value("${app.conversionflow.circuit-failure-threshold:5}") int circuitFailureThreshold,
            @Value("${app.conversionflow.circuit-open-ms:30000}") long circuitOpenMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitOpenMs = circuitOpenMs;
        this.successCounter = meterRegistry.counter("integration.conversionflow.lead.success");
        this.failureCounter = meterRegistry.counter("integration.conversionflow.lead.failure");
        this.retryCounter = meterRegistry.counter("integration.conversionflow.lead.retry");
        this.circuitOpenCounter = meterRegistry.counter("integration.conversionflow.lead.circuit.open");
        this.requestTimer = meterRegistry.timer("integration.conversionflow.lead.request.duration");
    }

    public void createLead(CreateLeadRequest request) {
        if (isCircuitOpen()) {
            circuitOpenCount.incrementAndGet();
            circuitOpenCounter.increment();
            throw new ConversionFlowIntegrationException("conversionflow_circuit_open", null);
        }

        ConversionFlowIntegrationException lastException = null;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                Timer.Sample sample = Timer.start();
                executeCreateLead(request);
                sample.stop(requestTimer);
                onSuccess();
                return;
            } catch (ConversionFlowIntegrationException exception) {
                lastException = exception;
                boolean retryable = isRetryable(exception);
                if (!retryable || attempt == retryMaxAttempts) {
                    break;
                }
                retryCount.incrementAndGet();
                retryCounter.increment();
                log.warn("conversionflow.lead.retry attempt={} externalId={} reason={}", attempt, request.externalId(), exception.getMessage());
                sleepBackoff();
            }
        }

        onFailure();
        throw lastException != null
                ? lastException
                : new ConversionFlowIntegrationException("conversionflow_lead_sync_failed", null);
    }

    private void executeCreateLead(CreateLeadRequest request) {
        RestClient.RequestBodySpec post = restClient.post()
                .uri("/leads")
                .contentType(MediaType.APPLICATION_JSON);

        if (!apiKey.isBlank()) {
            post = post.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        try {
            post.body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                return;
            }
            throw new ConversionFlowIntegrationException("conversionflow_lead_sync_failed", exception);
        } catch (RestClientException exception) {
            throw new ConversionFlowIntegrationException("conversionflow_lead_sync_failed", exception);
        }
    }

    private boolean isRetryable(ConversionFlowIntegrationException exception) {
        if (exception.getCause() instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            return status == 408 || status == 429 || status >= 500;
        }
        return true;
    }

    private boolean isCircuitOpen() {
        Instant openedAt = circuitOpenedAt;
        if (openedAt == null) {
            return false;
        }
        if (Instant.now().isBefore(openedAt.plusMillis(circuitOpenMs))) {
            return true;
        }
        circuitOpenedAt = null;
        consecutiveFailures.set(0);
        return false;
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        circuitOpenedAt = null;
        successCount.incrementAndGet();
        successCounter.increment();
        log.info("conversionflow.lead.success");
    }

    private void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        failureCount.incrementAndGet();
        failureCounter.increment();
        log.error("conversionflow.lead.failure consecutiveFailures={}", failures);
        if (failures >= circuitFailureThreshold) {
            circuitOpenedAt = Instant.now();
            consecutiveFailures.set(0);
            circuitOpenCount.incrementAndGet();
            circuitOpenCounter.increment();
            log.error("conversionflow.lead.circuit.open");
        }
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ConversionFlowIntegrationException("conversionflow_retry_interrupted", interruptedException);
        }
    }

    public ConversionFlowClientDiagnostics diagnostics() {
        return new ConversionFlowClientDiagnostics(
                isCircuitOpen(),
                consecutiveFailures.get(),
                successCount.get(),
                failureCount.get(),
                retryCount.get(),
                circuitOpenCount.get()
        );
    }
}
