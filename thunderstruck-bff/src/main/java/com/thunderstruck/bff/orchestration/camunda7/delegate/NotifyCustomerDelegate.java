package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("notifyCustomerDelegate")
@RequiredArgsConstructor
public class NotifyCustomerDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        SimulationSupport.failIfRequested(execution, "NOTIFICATION");
        String externalId = (String) execution.getVariable("externalId");
        String acao = (String) execution.getVariable("acao");
        String queue = (String) execution.getVariable("targetQueue");
        String status = "escalar".equalsIgnoreCase(acao) ? "ESCALATED" : "RESOLVED";
        trackingService.track(externalId, "NOTIFICATION_SENT", "camunda7", "CASE_CREATED", status,
                "Notificação enviada. acao=" + acao + ", queue=" + queue).subscribe();
    }
}
