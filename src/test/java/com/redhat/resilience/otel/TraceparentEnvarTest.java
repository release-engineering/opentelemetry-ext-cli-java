package com.redhat.resilience.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TraceparentEnvarTest
{
    @BeforeAll
    public static void staticSetup()
    {
        Resource resource = Resource.getDefault()
                                    .merge( Resource.create( Attributes.of( ResourceAttributes.SERVICE_NAME,
                                                                            "traceparent-envar-test" ) ) );

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().setResource( resource ).build();

        // NOTE the use of EnvarExtractingPropagator here
        EnvarExtractingPropagator.getInstance();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                                                         .setTracerProvider( sdkTracerProvider )
                                                         .setPropagators( ContextPropagators.create(
                                                                         EnvarExtractingPropagator.getInstance() ) )
                                                         .buildAndRegisterGlobal();

        //        Context extract = EnvarExtractingPropagator.getInstance().extract( Context.root(), null, null );
        //        extract.makeCurrent();
    }

    @Test
    public void createExecutionChildSpan( TestInfo testInfo )
    {
        System.out.println("\n\n" + testInfo.getDisplayName());

        Context parentContext = EnvarExtractingPropagator.getInstance().extract( Context.root(), null, null );
        Span span = GlobalOpenTelemetry.get()
                                       .getTracer( "traceparent-test" )
                                       .spanBuilder( "run" )
                                       .setParent( parentContext )
                                       .startSpan();
        //        Span span = OpenTelemetry.propagating( ContextPropagators.create( EnvarExtractingPropagator.getInstance() ) )
        //                                 .getTracer( "foo" )
        //                                 .spanBuilder( "foo" )
        //                                 .setParent( EnvarExtractingPropagator.getInstance()
        //                                                                      .extract( Context.root(), null, null ) )
        //                                 .startSpan();
        span.makeCurrent();

        Span current = Span.current();
        System.out.println( "Current span: " + current );

        assertNotNull( current, "No current span!" );
        //        assertTrue( current.getSpanContext().isValid() );

        // Sample data taken from W3C trace context spec
        assertEquals( "0af7651916cd43dd8448eb211c80319c", current.getSpanContext().getTraceId(), "Wrong trace ID" );

        // NOTE: This is the PARENT span, not the root span created in this execution!
        //        assertEquals( "b9c7c989f97918e1", current.getSpanContext().getSpanId(), "Wrong span ID" );

        TraceState traceState = current.getSpanContext().getTraceState();
        assertEquals( 2, traceState.size(), "Wrong trace state" );
        assertEquals( "t61rcWkgMzE", traceState.get( "congo" ), "Cannot find congo trace state" );
        assertEquals( "00f067aa0ba902b7", traceState.get( "rojo" ), "Cannot find rojo trace state" );
    }

    @Test
    public void reuseEnvarSpan(TestInfo testInfo)
    {
        System.out.println("\n\n" + testInfo.getDisplayName());

        Span span = OpenTelemetry.propagating( ContextPropagators.create( EnvarExtractingPropagator.getInstance() ) )
                                 .getTracer( "foo" )
                                 .spanBuilder( "foo" )
                                 .setParent( EnvarExtractingPropagator.getInstance()
                                                                      .extract( Context.root(), null, null ) )
                                 .startSpan();
        span.makeCurrent();

        Span current = Span.current();
        System.out.println( "Current span: " + current );

        assertNotNull( current, "No current span!" );
        //        assertTrue( current.getSpanContext().isValid() );

        // Sample data taken from W3C trace context spec
        assertEquals( "0af7651916cd43dd8448eb211c80319c", current.getSpanContext().getTraceId(), "Wrong trace ID" );

        // NOTE: This is the PARENT span, not the root span created in this execution!
        assertEquals( "b9c7c989f97918e1", current.getSpanContext().getSpanId(), "Wrong span ID" );

        TraceState traceState = current.getSpanContext().getTraceState();
        assertEquals( 2, traceState.size(), "Wrong trace state" );
        assertEquals( "t61rcWkgMzE", traceState.get( "congo" ), "Cannot find congo trace state" );
        assertEquals( "00f067aa0ba902b7", traceState.get( "rojo" ), "Cannot find rojo trace state" );
    }
}
