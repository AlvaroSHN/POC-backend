package com.thunderstruck.bff.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Camunda7ZeebeAutoConfigDisabler implements EnvironmentPostProcessor, Ordered {

    private static final String ORCHESTRATION_ENGINE = "thunderstruck.orchestration.engine";
    private static final String SPRING_AUTOCONFIG_EXCLUDE = "spring.autoconfigure.exclude";
    private static final Set<String> ZEEBE_AUTOCONFIGS = Set.of(
            "io.camunda.zeebe.spring.client.CamundaAutoConfiguration",
            "io.camunda.zeebe.spring.client.configuration.ZeebeClientAllAutoConfiguration"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String engine = environment.getProperty(ORCHESTRATION_ENGINE, "camunda7");
        if (!"camunda7".equalsIgnoreCase(engine)) {
            return;
        }

        Set<String> excludes = new LinkedHashSet<>();
        String configuredExcludes = environment.getProperty(SPRING_AUTOCONFIG_EXCLUDE, "");
        if (!configuredExcludes.isBlank()) {
            Arrays.stream(configuredExcludes.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(excludes::add);
        }
        excludes.addAll(ZEEBE_AUTOCONFIGS);

        environment.getPropertySources().addFirst(new MapPropertySource(
                "camunda7-zeebe-disable",
                Map.of(SPRING_AUTOCONFIG_EXCLUDE, String.join(",", excludes))
        ));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
