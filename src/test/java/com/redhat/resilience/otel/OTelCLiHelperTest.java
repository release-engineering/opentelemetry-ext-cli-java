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
    static
    {
        System.setProperty( org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE" );
    }

    @AfterEach
    public void otelTeardown()
    {
        OTelCLIHelper.stopOTel();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    public void verifyDoubleStart()
    {
        OTelCLIHelper.startOTel( "cli-test", SimpleSpanProcessor.create( new TestSpanExporter() ) );

        IllegalStateException thrown = assertThrows(
                        IllegalStateException.class,
                        () -> OTelCLIHelper.startOTel( "traceparent-envar-cli-test",
                                           SimpleSpanProcessor.create( new TestSpanExporter() ) )
        );

        assertTrue(thrown.getMessage().contains("startOTel has already been called"));
    }

    @Test
    public void verifyDoubleStop()
    {
        OTelCLIHelper.startOTel( "cli-test", SimpleSpanProcessor.create( new TestSpanExporter() ) );
        OTelCLIHelper.stopOTel();
        OTelCLIHelper.stopOTel();
    }
}
