/*
 * Copyright (C) 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class to make setup / teardown of OpenTelemetry tracing simpler for CLI tools.
 */
@Slf4j
@UtilityClass
public class OTelCLIHelper {
    private SpanProcessor spanProcessor;

    private Span root = null;

    /**
     * Setup a {@link OtlpGrpcSpanExporter} exporter with the given endpoint.
     *
     * @param endpoint The gRPC endpoint for sending span data
     * @return The {@link OtlpGrpcSpanExporter} instance
     */
    public SpanExporter defaultSpanExporter(String endpoint) {
        return OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
    }

    /**
     * Setup a {@link BatchSpanProcessor} with the supplied {@link SpanExporter}.
     *
     * @param exporter The {@link SpanExporter}, which MAY come from {@link OTelCLIHelper#defaultSpanExporter}
     * @return The {@link BatchSpanProcessor} instance
     */
    public SpanProcessor defaultSpanProcessor(SpanExporter exporter) {
        return BatchSpanProcessor.builder(exporter).build();
    }

    /**
     * Setup {@link GlobalOpenTelemetry} using the provided service name and span processor (which contains an
     * exporter).
     * This will also ininitialize with the {@link EnvarExtractingPropagator} context propagator, which knows how to set
     * W3C HTTP headers for propagating context downstream, but reads similar fields from {@link System#getenv()}.
     * <p>
     * When the {@link GlobalOpenTelemetry} setup is done, <b>this method will also start a root span</b>, which enables
     * the CLI execution to use {@link Span#current()} to set attributes directly with no further setup required.
     *
     * @param serviceName This translates into 'service.name' in the span, which is usually required for span validity
     * @param processor This is a span processor that determines how spans are exported
     */
    public void startOTel(String serviceName, SpanProcessor processor) {
        startOTel(serviceName, "cli-execution", processor);
    }

    /**
     * Setup {@link GlobalOpenTelemetry} using the provided service name and span processor (which contains an
     * exporter).
     * This will also ininitialize with the {@link EnvarExtractingPropagator} context propagator, which knows how to set
     * W3C HTTP headers for propagating context downstream, but reads similar fields from {@link System#getenv()}.
     * <p>
     * When the {@link GlobalOpenTelemetry} setup is done, <b>this method will also start a root span</b>, which enables
     * the CLI execution to use {@link Span#current()} to set attributes directly with no further setup required.
     *
     * @param serviceName This translates into 'service.name' in the span, which is usually required for span validity
     * @param commandName This is used to name the new span
     * @param processor This is a span processor that determines how spans are exported
     */
    public void startOTel(String serviceName, String commandName, SpanProcessor processor) {
        if (spanProcessor != null) {
            throw new IllegalStateException("startOTel has already been called");
        }
        if (serviceName == null) {
            throw new RuntimeException("serviceName must be passed in");
        }
        if (commandName == null) {
            commandName = serviceName;
        }

        spanProcessor = processor;

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(processor)
                .setResource(resource)
                .build();

        // NOTE the use of EnvarExtractingPropagator here
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(EnvarExtractingPropagator.getInstance()))
                .buildAndRegisterGlobal();

        Context parentContext = EnvarExtractingPropagator.getInstance().extract(Context.current(), null, null);
        root = openTelemetry.getTracer(serviceName).spanBuilder(commandName).setParent(parentContext).startSpan();

        root.makeCurrent();
        log.debug(
                "Running with traceId {} spanId {}",
                Span.current().getSpanContext().getTraceId(),
                Span.current().getSpanContext().getSpanId());
    }

    /**
     * Return whether this is enabled
     *
     * @return a boolean with the current enabled status.
     */
    public boolean otelEnabled() {
        return spanProcessor != null;
    }

    /**
     * Shutdown the span processor, giving it some time to flush any pending spans out to the exporter.
     */
    public void stopOTel() {
        if (otelEnabled()) {
            log.debug("Finishing OpenTelemetry instrumentation for {}", root);
            if (root != null) {
                root.end();
            }
            spanProcessor.close();
            spanProcessor = null;
        }
    }
}
