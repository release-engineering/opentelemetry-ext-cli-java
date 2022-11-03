package com.redhat.resilience.otel.fixture;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public final class TestSpanExporterConfigurableProvider
                implements ConfigurableSpanExporterProvider
{
    @Override
    public SpanExporter createExporter( ConfigProperties configProperties )
    {
        return new TestSpanExporter();
    }

    @Override
    public String getName()
    {
        return "test";
    }
}
