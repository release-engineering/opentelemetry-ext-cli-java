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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.redhat.resilience.otel.fixture.TestSpanExporter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

public class TraceparentEnvarCLITest {
    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";

    private static final String SPAN_ID = "b9c7c989f97918e1";

    private EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeEach
    public void otelSetup() throws Exception {
        TestSpanExporter.clear();

        // normally you'd probably call OtelCLIHelper.defaultSpanProcessor() and OtelCLIHelper.defaultSpanExporter()
        // We have to use a SimpleSpanProcessor and the TestSpanExporter to ensure the span data is recorded immediately

        // See https://www.w3.org/TR/trace-context/#relationship-between-the-headers
        environmentVariables.set("TRACEPARENT", "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01")
                .and("TRACESTATE", "rojo=00f067aa0ba902b7,congo=t61rcWkgMzE")
                .execute(
                        () -> OTelCLIHelper.startOTel(
                                "traceparent-envar-cli-test",
                                SimpleSpanProcessor.create(new TestSpanExporter())));
    }

    @AfterEach
    public void otelTeardown() {
        OTelCLIHelper.stopOTel();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    public void createExecutionChildSpan(TestInfo testInfo) {
        System.out.println("\n\n" + testInfo.getDisplayName());

        Span current = Span.current();
        assertNotNull(current);

        System.out.println("Current span: " + current);

        assertNotNull(current, "No current span!");

        // Sample data taken from W3C trace context spec
        assertEquals(TRACE_ID, current.getSpanContext().getTraceId(), "Wrong trace ID");

        // NOTE: This is the PARENT span, not the root span created in this execution!
        //        assertEquals( "b9c7c989f97918e1", current.getSpanContext().getSpanId(), "Wrong span ID" );

        TraceState traceState = current.getSpanContext().getTraceState();
        assertEquals(2, traceState.size(), "Wrong trace state");
        assertEquals("t61rcWkgMzE", traceState.get("congo"), "Cannot find congo trace state");
        assertEquals("00f067aa0ba902b7", traceState.get("rojo"), "Cannot find rojo trace state");

        // regular execution ends...

        current.end();

        List<SpanData> spanData = TestSpanExporter.getSpans();
        assertEquals(1, spanData.size(), "Incorrect span count!");
        assertEquals(TRACE_ID, spanData.get(0).getTraceId(), "Incorrect trace ID in span output");
        assertEquals(SPAN_ID, spanData.get(0).getParentSpanId(), "Incorrect parent span ID in span output");
    }

    @Test
    public void createExecutionChildSpanWithStop(TestInfo testInfo) {
        System.out.println("\n\n" + testInfo.getDisplayName());

        Span current = Span.current();
        assertNotNull(current);

        System.out.println("Current span: " + current);

        assertNotNull(current, "No current span!");

        // Sample data taken from W3C trace context spec
        assertEquals(TRACE_ID, current.getSpanContext().getTraceId(), "Wrong trace ID");

        // NOTE: This is the PARENT span, not the root span created in this execution!
        //        assertEquals( "b9c7c989f97918e1", current.getSpanContext().getSpanId(), "Wrong span ID" );

        TraceState traceState = current.getSpanContext().getTraceState();
        assertEquals(2, traceState.size(), "Wrong trace state");
        assertEquals("t61rcWkgMzE", traceState.get("congo"), "Cannot find congo trace state");
        assertEquals("00f067aa0ba902b7", traceState.get("rojo"), "Cannot find rojo trace state");

        // regular execution ends...

        OTelCLIHelper.stopOTel();

        List<SpanData> spanData = TestSpanExporter.getSpans();
        assertEquals(1, spanData.size(), "Incorrect span count!");
        assertEquals(TRACE_ID, spanData.get(0).getTraceId(), "Incorrect trace ID in span output");
        assertEquals(SPAN_ID, spanData.get(0).getParentSpanId(), "Incorrect parent span ID in span output");
    }
}
