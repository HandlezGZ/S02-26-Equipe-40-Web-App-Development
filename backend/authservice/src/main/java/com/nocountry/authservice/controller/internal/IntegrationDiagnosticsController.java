package com.nocountry.authservice.controller.internal;

import com.nocountry.authservice.integration.conversionflow.ConversionFlowClientDiagnostics;
import com.nocountry.authservice.integration.conversionflow.ConversionFlowLeadClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/diagnostics")
public class IntegrationDiagnosticsController {

    private final ConversionFlowLeadClient conversionFlowLeadClient;

    public IntegrationDiagnosticsController(ConversionFlowLeadClient conversionFlowLeadClient) {
        this.conversionFlowLeadClient = conversionFlowLeadClient;
    }

    @GetMapping("/conversionflow")
    public ResponseEntity<ConversionFlowClientDiagnostics> conversionFlowDiagnostics() {
        return ResponseEntity.ok(conversionFlowLeadClient.diagnostics());
    }
}
