package com.thunderstruck.bff.orchestration.camunda7.delegate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SimulationSupport {

    private SimulationSupport() {
    }

    public static void failIfRequested(DelegateExecution execution, String step) {
        String failAt = (String) execution.getVariable("simulateFailAt");
        if (failAt != null && failAt.equalsIgnoreCase(step)) {
            throw new BpmnError("SIM_ERROR", "Falha simulada no step " + step);
        }
    }

    public static String transformDetails(Map<String, Object> before, Map<String, Object> after) {
        return "before=" + before + " | after=" + after;
    }

    public static Map<String, Object> snapshot(DelegateExecution execution, String... keys) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String k : keys) {
            out.put(k, execution.getVariable(k));
        }
        return out;
    }
}
