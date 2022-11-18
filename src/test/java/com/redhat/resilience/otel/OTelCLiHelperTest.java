package com.redhat.resilience.otel;

import com.redhat.resilience.otel.fixture.TestSpanExporter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OTelCLiHelperTest
{
    @AfterEach
    public void otelTeardown()
    {
        OtelCLIHelper.stopOtel();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    public void verifyDoubleStart()
    {
        OtelCLIHelper.startOtel( "traceparent-envar-cli-test", SimpleSpanProcessor.create( new TestSpanExporter() ) );

        IllegalStateException thrown = assertThrows(
                        IllegalStateException.class,
                        () -> OtelCLIHelper.startOtel( "traceparent-envar-cli-test", SimpleSpanProcessor.create( new TestSpanExporter() ) )
        );

        assertTrue(thrown.getMessage().contains("startOtel has already been called"));
    }
}
