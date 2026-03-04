package com.nocountry.conversionflow.conversionflow_api.infrastructure.messaging.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.application.exception.DuplicateLeadException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.InvalidInputException;
import com.nocountry.conversionflow.conversionflow_api.application.usecase.CreateLeadUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventListener.class);

    private final ObjectMapper objectMapper;
    private final CreateLeadUseCase createLeadUseCase;

    public UserRegisteredEventListener(ObjectMapper objectMapper, CreateLeadUseCase createLeadUseCase) {
        this.objectMapper = objectMapper;
        this.createLeadUseCase = createLeadUseCase;
    }

    @RabbitListener(queues = "${async.auth.user-registered-queue:auth.user.registered.v1}")
    public void onMessage(String rawMessage) {
        try {
            UserRegisteredEventEnvelope event = objectMapper.readValue(rawMessage, UserRegisteredEventEnvelope.class);
            validate(event);

            UserRegisteredEventEnvelope.Payload payload = event.payload();
            UserRegisteredEventEnvelope.Attribution attribution = payload.attribution();

            createLeadUseCase.execute(
                    payload.authUserId(),
                    payload.email(),
                    attribution != null ? attribution.gclid() : null,
                    attribution != null ? attribution.fbclid() : null,
                    attribution != null ? attribution.fbp() : null,
                    attribution != null ? attribution.fbc() : null,
                    attribution != null ? attribution.utmSource() : null,
                    attribution != null ? attribution.utmCampaign() : null
            );

            log.info("auth.event.userRegistered.processed eventId={} authUserId={}", event.eventId(), payload.authUserId());
        } catch (InvalidInputException exception) {
            log.warn("auth.event.userRegistered.invalid message={} reason={}", rawMessage, exception.getMessage());
        } catch (DuplicateLeadException exception) {
            log.info("auth.event.userRegistered.duplicate message={}", rawMessage);
        } catch (Exception exception) {
            log.error("auth.event.userRegistered.processing_error message={}", rawMessage, exception);
            throw new RuntimeException("user_registered_event_processing_failed", exception);
        }
    }

    private void validate(UserRegisteredEventEnvelope event) {
        if (event == null) {
            throw new InvalidInputException("event_missing");
        }
        if (!"UserRegistered".equals(event.eventName())) {
            throw new InvalidInputException("unexpected_event_name");
        }
        if (event.payload() == null) {
            throw new InvalidInputException("payload_missing");
        }
        if (isBlank(event.payload().authUserId())) {
            throw new InvalidInputException("auth_user_id_missing");
        }
        if (isBlank(event.payload().email())) {
            throw new InvalidInputException("email_missing");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
