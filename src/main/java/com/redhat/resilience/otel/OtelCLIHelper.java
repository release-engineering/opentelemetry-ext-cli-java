package com.redhat.resilience.otel;

import com.redhat.resilience.otel.internal.EnvarExtractingPropagator;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class OtelCLIHelper
{
    private static SpanProcessor spanProcessor;

    public static SpanExporter defaultSpanExporter( String endpoint )
    {
        return OtlpGrpcSpanExporter.builder().setEndpoint( endpoint ).build();
    }

    public static SpanProcessor defaultSpanProcessor( SpanExporter exporter )
    {
        return BatchSpanProcessor.builder( exporter ).build();
    }

    public static void startOtel( String serviceName, SpanProcessor processor )
    {
        spanProcessor = processor;

        Resource resource = Resource.getDefault()
                                    .merge( Resource.create( Attributes.of( ResourceAttributes.SERVICE_NAME,
                                                                            serviceName ) ) );

        SdkTracerProvider sdkTracerProvider =
                        SdkTracerProvider.builder().addSpanProcessor( processor ).setResource( resource ).build();

        // NOTE the use of EnvarExtractingPropagator here
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                                                         .setTracerProvider( sdkTracerProvider )
                                                         .setPropagators( ContextPropagators.create(
                                                                         EnvarExtractingPropagator.getInstance() ) )
                                                         .buildAndRegisterGlobal();

        Context parentContext = EnvarExtractingPropagator.getInstance().extract( Context.root(), null, null );
        Span root = GlobalOpenTelemetry.get()
                                       .getTracer( serviceName )
                                       .spanBuilder( "cli-execution" )
                                       .setParent( parentContext )
                                       .startSpan();
        root.makeCurrent();
    }

    public static void stopOtel()
    {
        if ( spanProcessor != null )
        {
            spanProcessor.close();
        }
    }
}
