package com.redhat.resilience.otel.fixture;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.HashMap;
import java.util.Map;

public class CustomTestProfile implements QuarkusTestProfile
{
    @Override
    public Map<String, String> getConfigOverrides()
    {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("quarkus.opentelemetry.enabled", "true");
        overrides.put("quarkus.opentelemetry.propagators", "envar");

        return overrides;
    }
}
