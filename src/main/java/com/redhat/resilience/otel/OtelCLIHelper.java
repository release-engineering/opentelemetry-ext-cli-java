/*
 * Copyright Red Hat
 * SPDX-License-Identifier: Apache-2.0
 */

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

/**
 * Helper class to make setup / teardown of Opentelemetry tracing simpler for CLI tools.
 */
public class OtelCLIHelper
{
    private static SpanProcessor spanProcessor;

    /**
     * Setup a {@link OtlpGrpcSpanExporter} exporter with the given endpoint.
     *
     * @param endpoint The gRPC endpoint for sending span data
     * @return The {@link OtlpGrpcSpanExporter} instance
     */
    public static SpanExporter defaultSpanExporter( String endpoint )
    {
        return OtlpGrpcSpanExporter.builder().setEndpoint( endpoint ).build();
    }

    /**
     * Setup a {@link BatchSpanProcessor} with the supplied {@link SpanExporter}.
     *
     * @param exporter The {@link SpanExporter}, which MAY come from {@link OtelCLIHelper#defaultSpanExporter}
     * @return The {@link BatchSpanProcessor} instance
     */
    public static SpanProcessor defaultSpanProcessor( SpanExporter exporter )
    {
        return BatchSpanProcessor.builder( exporter ).build();
    }

    /**
     * Setup {@link GlobalOpenTelemetry} using the provided service name and span processor (which contains an exporter).
     * This will also ininitialize with the {@link EnvarExtractingPropagator} context propagator, which knows how to set
     * W3C HTTP headers for propagating context downstream, but reads similar fields from {@link System#getenv()}.
     * <p>
     * When the {@link GlobalOpenTelemetry} setup is done, <b>this method will also start a root span</b>, which enables
     * the CLI execution to use {@link Span#current()} to set attributes directly with no further setup required.
     *
     * @param serviceName This translates into 'service.name' in the span, which is usually required for span validity"
     * @param processor This is a span processor that determines how spans are exported
     */
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

    /**
     * Shutdown the span processor, giving it some time to flush any pending spans out to the exporter.
     */
    public static void stopOtel()
    {
        if ( spanProcessor != null )
        {
            spanProcessor.close();
        }
    }
}
