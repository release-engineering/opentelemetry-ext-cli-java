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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.sdk.trace.data.SpanData;

@Path("/spans")
public class QuarkusSpansResource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @GET
    public String get() {
        StringBuilder sb = new StringBuilder();
        List<SpanData> spans = TestSpanExporter.getSpans();
        logger.info("Got {} spans:\n{}", spans.size(), spans);

        spans.forEach(spanData -> {
            if (sb.length() > 0) {
                sb.append("\n");
            }

            sb.append(spanData.getTraceId())
                    .append(',')
                    .append(spanData.getParentSpanId())
                    .append(',')
                    .append(spanData.getSpanId())
                    .append(',')
                    .append(spanData.getName())
                    .append(',')
                    .append(spanData.getKind().name());
        });

        //        logger.info( "In QuarkusSpansResource, returning spans string:\n {}", sb );
        return sb.toString();
    }
}
