package com.redhat.resilience.otel.fixture;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Just hold all the span data for later inspection at the end of a test.
 */
public class TestSpanExporter implements SpanExporter
{
    private static final List<SpanData> SPANS = new ArrayList<>();

    @Override
    public CompletableResultCode export( Collection<SpanData> spans )
    {
        SPANS.addAll( spans );
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush()
    {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown()
    {
        return flush();
    }

    public static void clear()
    {
        SPANS.clear();
    }

    public static List<SpanData> getSpans()
    {
        return Collections.unmodifiableList( SPANS );
    }
}
