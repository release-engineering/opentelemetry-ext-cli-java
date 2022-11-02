/*
 * Copyright Red Hat
 * SPDX-License-Identifier: Apache-2.0
 */

package com.redhat.resilience.otel;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import static com.redhat.resilience.otel.OtelContextUtil.extractContextFromTraceParent;
import static com.redhat.resilience.otel.OtelContextUtil.extractTraceState;

/**
 * {@link TextMapPropagator} implementation (for propagating trace context) that consumes environment variables during
 * extraction, and injects W3C headers for downstream calls. This is intended to be used in command-line tooling,
 * and is intended to be compatible with the Jenkins Opentelemetry Plugin.
 * <br/>
 * This <b>ONLY</b> works with single-execution tools; if your software uses a processing loop and runs as a daemon of
 * some sort, <b>DO NOT USE THIS.</b>
 * <p>
 * See also: https://github.com/jenkinsci/opentelemetry-plugin/blob/master/docs/job-traces.md#environment-variables-for-trace-context-propagation-and-integrations
 */
public class EnvarExtractingPropagator
                implements TextMapPropagator
{
    private static final String ENVAR_TRACE_PARENT = "TRACEPARENT";

    private static final String ENVAR_TRACE_STATE = "TRACESTATE";

    private static final String ENVAR_TRACE_ID = "TRACE_ID";

    private static final String ENVAR_SPAN_ID = "SPAN_ID";

    private static final EnvarExtractingPropagator INSTANCE = new EnvarExtractingPropagator();

    private EnvarExtractingPropagator()
    {
    }

    public static EnvarExtractingPropagator getInstance()
    {
        return INSTANCE;
    }

    /**
     * Return the set of fields we will inject. These are intended to be fully compatible with the W3C trace context, so
     * it delegates directly to {@link W3CTraceContextPropagator#fields()}
     */
    @Override
    public Collection<String> fields()
    {
        return W3CTraceContextPropagator.getInstance().fields();
    }

    /**
     * Delegate to {@link W3CTraceContextPropagator#inject(Context, Object, TextMapSetter)} for field injection in the
     * outgoing context.
     */
    @Override
    public <C> void inject( Context context, C c, TextMapSetter<C> textMapSetter )
    {
        W3CTraceContextPropagator.getInstance().inject( context, c, textMapSetter );
    }

    /**
     * Extract trace context from system environment variables. This <b>ONLY</b> works with single-execution tools;
     * if your system uses a processing loop and runs as a daemon of some sort, <b>DO NOT USE THIS.</b>
     * <br/>
     * This extraction will reuse the TRACEPARENT, TRACESTATE, TRACE_ID, and SPAN_ID environment variables produced
     * by Jenkins via the Jenkins Opentelemetry Plugin. It will <b>prefer</b> TRACEPARENT then fall back to
     * [TRACE_ID + SPAN_ID]. If it can establish a basic trace context from those, it will look for TRACESTATE to
     * augment the base context information.
     */
    @Override
    public <C> Context extract( Context context, C carrier, TextMapGetter<C> getter )
    {
        System.out.println("\n\n\n\nEXTRACT\n\n\n");

        if ( context == null )
        {
            return Context.root();
        }
        if ( getter == null )
        {
            return context;
        }

        SpanContext spanContext = extractImpl();
        if ( !spanContext.isValid() )
        {
            return context;
        }

        return context.with( Span.wrap( spanContext ) );
    }

    private static <C> SpanContext extractImpl()
    {
        final Logger logger = LoggerFactory.getLogger( OtelContextUtil.class );

        Map<String, String> envMap = System.getenv();

        System.out.println("\n\n\n\nENVARS: " + envMap + "\n\n\n");

        SpanContext contextFromParent = null;

        String traceParent = envMap.get( ENVAR_TRACE_PARENT );
        if ( traceParent != null )
        {
            contextFromParent = extractContextFromTraceParent( traceParent );
        }

        if ( contextFromParent == null )
        {
            String traceId = envMap.get( ENVAR_TRACE_ID );
            String parentSpanId = envMap.get( ENVAR_SPAN_ID );
            if ( traceId != null && !traceId.isEmpty() && parentSpanId != null && !parentSpanId.isEmpty() )
            {
                contextFromParent = SpanContext.createFromRemoteParent( traceId, parentSpanId, TraceFlags.getDefault(),
                                                                        TraceState.getDefault() );
            }
        }

        if ( contextFromParent == null )
        {
            return SpanContext.getInvalid();
        }
        else if ( !contextFromParent.isValid() )
        {
            return contextFromParent;
        }

        String traceStateHeader = envMap.get( ENVAR_TRACE_STATE );
        if ( traceStateHeader == null || traceStateHeader.isEmpty() )
        {
            return contextFromParent;
        }

        try
        {
            TraceState traceState = extractTraceState( traceStateHeader );
            return SpanContext.createFromRemoteParent( contextFromParent.getTraceId(), contextFromParent.getSpanId(),
                                                       contextFromParent.getTraceFlags(), traceState );
        }
        catch ( IllegalArgumentException e )
        {
            logger.debug( "Unparseable tracestate header. Returning span context without state." );
            return contextFromParent;
        }
    }
}
