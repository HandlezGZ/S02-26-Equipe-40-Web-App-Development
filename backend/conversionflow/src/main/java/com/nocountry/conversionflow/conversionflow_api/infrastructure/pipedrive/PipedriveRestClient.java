package com.nocountry.conversionflow.conversionflow_api.infrastructure.pipedrive;

import com.nocountry.conversionflow.conversionflow_api.config.properties.PipedriveProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class PipedriveRestClient implements PipedriveClient {

    private final PipedriveProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public PipedriveRestClient(PipedriveProperties properties) {
        this.properties = properties;
    }

    @Override
    public void syncConvertedLead(LeadConvertedEvent event) {

        Long personId = findOrCreatePerson(event);
        createDeal(event, personId);
    }

    private Long findOrCreatePerson(LeadConvertedEvent event) {

        String searchUrl = properties.getBaseUrl()
                + "/persons/search?term="
                + event.getEmail()
                + "&api_token=" + properties.getApiToken();

        ResponseEntity<Map> response =
                restTemplate.getForEntity(searchUrl, Map.class);

        if (response.getBody() != null
                && response.getBody().containsKey("data")) {

            Map data = (Map) response.getBody().get("data");

            if (data != null && data.containsKey("items")) {
                var items = (java.util.List<Map>) data.get("items");

                if (!items.isEmpty()) {
                    Map item = items.get(0);
                    Map person = (Map) item.get("item");
                    return Long.valueOf(person.get("id").toString());
                }
            }
        }

        return createPerson(event);
    }

    private Long createPerson(LeadConvertedEvent event) {

        String url = properties.getBaseUrl()
                + "/persons?api_token=" + properties.getApiToken();

        Map<String, Object> body = new HashMap<>();
        body.put("name", event.getEmail());
        body.put("email", event.getEmail());

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, body, Map.class);

        Map data = (Map) response.getBody().get("data");

        return Long.valueOf(data.get("id").toString());
    }

    private void createDeal(LeadConvertedEvent event, Long personId) {

        String url = properties.getBaseUrl()
                + "/deals?api_token=" + properties.getApiToken();

        Map<String, Object> body = new HashMap<>();
        body.put("title", "Converted Lead - " + event.getExternalId());
        body.put("value", event.getConvertedAmount());
        body.put("currency", "USD");
        body.put("person_id", personId);
        body.put("status", "won");

        restTemplate.postForEntity(url, body, Map.class);
    }
}