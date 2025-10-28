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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Just hold all the span data for later inspection at the end of a test.
 */
public class TestSpanExporter implements SpanExporter {
    private static final List<SpanData> SPANS = new ArrayList<>();

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        record(spans);
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return flush();
    }

    public static void record(Collection<SpanData> spans) {
        SPANS.addAll(spans);
    }

    public static void clear() {
        SPANS.clear();
    }

    public static List<SpanData> getSpans() {
        return Collections.unmodifiableList(SPANS);
    }

}
