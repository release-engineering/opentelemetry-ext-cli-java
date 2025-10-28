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
package com.redhat.resilience.otel.fixture;

import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.ReadableSpan;

@Path("/test")
public class QuarkusTestResource {
    @GET
    public Response test() {
        Span span = Span.current();

        Span client = GlobalOpenTelemetry.get()
                .getTracer("test-client")
                .spanBuilder("get-test-resource")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("service.name", "sidecar")
                .startSpan();

        client.end();

        TestSpanExporter.record(
                Arrays.asList(((ReadableSpan) span).toSpanData(), ((ReadableSpan) client).toSpanData()));

        return Response.ok().build();
    }
}
