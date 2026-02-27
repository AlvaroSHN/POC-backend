package com.thunderstruck.bff.service;

import java.util.Map;

public interface OrchestrationEngine {
    void startProcess(String processDefinitionKey, Map<String, Object> variables);
}
