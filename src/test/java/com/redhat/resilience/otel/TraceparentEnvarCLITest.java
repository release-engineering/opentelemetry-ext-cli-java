package com.redhat.resilience.otel;

import com.redhat.resilience.otel.fixture.TestSpanExporter;
import com.redhat.resilience.otel.internal.EnvarExtractingPropagator;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TraceparentEnvarCLITest
{
    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";

    private static final String SPAN_ID = "b9c7c989f97918e1";

    @BeforeEach
    public void otelSetup()
    {
        TestSpanExporter.clear();
        OtelCLIHelper.startOtel( "traceparent-envar-cli-test", SimpleSpanProcessor.create( new TestSpanExporter() ) );
    }

    @AfterEach
    public void otelTeardown()
    {
        OtelCLIHelper.stopOtel();
    }

    @Test
    public void createExecutionChildSpan( TestInfo testInfo )
    {
        System.out.println( "\n\n" + testInfo.getDisplayName() );

        // Start a child span, just to ensure nesting.

        // regular execution begins...

        Span current = Span.current();
        assertNotNull( current );

        System.out.println( "Current span: " + current );

        assertNotNull( current, "No current span!" );
        //        assertTrue( current.getSpanContext().isValid() );

        // Sample data taken from W3C trace context spec
        assertEquals( TRACE_ID, current.getSpanContext().getTraceId(), "Wrong trace ID" );

        // NOTE: This is the PARENT span, not the root span created in this execution!
        //        assertEquals( "b9c7c989f97918e1", current.getSpanContext().getSpanId(), "Wrong span ID" );

        TraceState traceState = current.getSpanContext().getTraceState();
        assertEquals( 2, traceState.size(), "Wrong trace state" );
        assertEquals( "t61rcWkgMzE", traceState.get( "congo" ), "Cannot find congo trace state" );
        assertEquals( "00f067aa0ba902b7", traceState.get( "rojo" ), "Cannot find rojo trace state" );

        // regular execution ends...

        current.end();

        List<SpanData> spanData = TestSpanExporter.getSpans();
        assertEquals( 1, spanData.size(), "Incorrect span count!" );
        assertEquals( TRACE_ID, spanData.get( 0 ).getTraceId(), "Incorrect trace ID in span output" );
    }

}
