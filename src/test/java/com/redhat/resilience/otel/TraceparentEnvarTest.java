package com.redhat.resilience.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TraceparentEnvarTest
{
    @BeforeAll
    public static void staticSetup()
    {
    }

    @Test
    public void run()
    {
        Resource resource = Resource.getDefault()
                                    .merge( Resource.create( Attributes.of( ResourceAttributes.SERVICE_NAME,
                                                                            "traceparent-envar-test" ) ) );

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().setResource( resource ).build();

        // NOTE the use of EnvarExtractingPropagator here
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                                                         .setTracerProvider( sdkTracerProvider )
                                                         .setPropagators( ContextPropagators.create(
                                                                         EnvarExtractingPropagator.getInstance() ) )
                                                         .buildAndRegisterGlobal();
        Tracer tracer =
                        openTelemetry.getTracer("traceparent-envar-test", "1.0.0");

        Span span = tracer.spanBuilder( "run-method" ).startSpan();
        span.makeCurrent();

        System.out.println( "run-method span: " + span );
        assertNotNull( span );
        assertTrue( span.getSpanContext().isValid() );


        Span current = Span.current();
        System.out.println( "\n\n\nCurent span: " + current );

        assertNotNull( current );
        assertTrue( current.getSpanContext().isValid() );


        // Sample data taken from W3C trace context spec
        assertEquals( "0af7651916cd43dd8448eb211c80319c", current.getSpanContext().getTraceId() );
        assertEquals( "b9c7c989f97918e1", current.getSpanContext().getSpanId() );

        TraceState traceState = current.getSpanContext().getTraceState();
        assertEquals( 2, traceState.size() );
        assertEquals( "t61rcWkgMzE", traceState.get( "congo" ) );
        assertEquals( "00f067aa0ba902b7", traceState.get( "rojo" ) );
    }
}
