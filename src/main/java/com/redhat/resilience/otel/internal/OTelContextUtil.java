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
package com.redhat.resilience.otel.internal;

import static io.opentelemetry.api.internal.Utils.checkArgument;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;

/**
 * Rip off of the utility functions from {@link W3CTraceContextPropagator} (version 1.6.0), for parsing
 * traceparent and tracestate headers. These headers are also used by the Jenkins OpenTelemetry Plugin, and form a
 * reasonable convention for passing trace context outside of HTTP...so, we'll reuse them.
 *
 * @see W3CTraceContextPropagator
 */
public final class OTelContextUtil {
    private static final int VERSION_SIZE = 2;
    private static final char TRACEPARENT_DELIMITER = '-';
    private static final int TRACEPARENT_DELIMITER_SIZE = 1;
    private static final int TRACE_ID_HEX_SIZE = TraceId.getLength();
    private static final int SPAN_ID_HEX_SIZE = SpanId.getLength();
    private static final int TRACE_OPTION_HEX_SIZE = TraceFlags.getLength();
    private static final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int SPAN_ID_OFFSET = TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int TRACE_OPTION_OFFSET = SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int TRACEPARENT_HEADER_SIZE = TRACE_OPTION_OFFSET + TRACE_OPTION_HEX_SIZE;
    private static final int TRACESTATE_MAX_MEMBERS = 32;
    private static final char TRACESTATE_KEY_VALUE_DELIMITER = '=';
    private static final char TRACESTATE_ENTRY_DELIMITER = ',';
    private static final Pattern TRACESTATE_ENTRY_DELIMITER_SPLIT_PATTERN = Pattern
            .compile("[ \t]*" + TRACESTATE_ENTRY_DELIMITER + "[ \t]*");
    private static final Set<String> VALID_VERSIONS;
    private static final String VERSION_00 = "00";

    static {
        // A valid version is 1 byte representing an 8-bit unsigned integer, version ff is invalid.
        VALID_VERSIONS = new HashSet<>();
        for (int i = 0; i < 255; i++) {
            String version = Long.toHexString(i);
            if (version.length() < 2) {
                version = '0' + version;
            }
            VALID_VERSIONS.add(version);
        }
    }

    private OTelContextUtil() {
    }

    /**
     * Parse a W3C-compliant traceparent header value, and return a {@link SpanContext} with the details, or else
     * {@link SpanContext#getInvalid()}. <b>NOTE:</b> It's worth realizing that traceparent doesn't HAVE TO come from
     * a HTTP request, if your software doesn't listen for HTTP requests (or has a different use case).
     *
     * @param traceparent The traceparent 'header' to extract from...this doesn't have to come from HTTP headers!
     * @return The SpanContext parsed from the traceparent, or else {@link SpanContext#getInvalid()}
     */
    public static SpanContext extractContextFromTraceParent(String traceparent) {
        final Logger logger = LoggerFactory.getLogger(OTelContextUtil.class);

        // TODO(bdrutu): Do we need to verify that version is hex and that
        // for the version the length is the expected one?
        boolean isValid = (traceparent.length() == TRACEPARENT_HEADER_SIZE
                || (traceparent.length() > TRACEPARENT_HEADER_SIZE
                        && traceparent.charAt(TRACEPARENT_HEADER_SIZE) == TRACEPARENT_DELIMITER))
                && traceparent.charAt(TRACE_ID_OFFSET - 1) == TRACEPARENT_DELIMITER
                && traceparent.charAt(SPAN_ID_OFFSET - 1) == TRACEPARENT_DELIMITER
                && traceparent.charAt(TRACE_OPTION_OFFSET - 1) == TRACEPARENT_DELIMITER;
        if (!isValid) {
            logger.debug("Unparseable traceparent header. Returning INVALID span context.");
            return SpanContext.getInvalid();
        }

        String version = traceparent.substring(0, 2);
        if (!VALID_VERSIONS.contains(version)) {
            return SpanContext.getInvalid();
        }
        if (version.equals(VERSION_00) && traceparent.length() > TRACEPARENT_HEADER_SIZE) {
            return SpanContext.getInvalid();
        }

        String traceId = traceparent.substring(TRACE_ID_OFFSET, TRACE_ID_OFFSET + TraceId.getLength());
        String spanId = traceparent.substring(SPAN_ID_OFFSET, SPAN_ID_OFFSET + SpanId.getLength());
        char firstTraceFlagsChar = traceparent.charAt(TRACE_OPTION_OFFSET);
        char secondTraceFlagsChar = traceparent.charAt(TRACE_OPTION_OFFSET + 1);

        if (!OtelEncodingUtils.isValidBase16Character(firstTraceFlagsChar)
                || !OtelEncodingUtils.isValidBase16Character(secondTraceFlagsChar)) {
            return SpanContext.getInvalid();
        }

        TraceFlags traceFlags = TraceFlags.fromByte(
                OtelEncodingUtils.byteFromBase16(firstTraceFlagsChar, secondTraceFlagsChar));
        return SpanContext.createFromRemoteParent(traceId, spanId, traceFlags, TraceState.getDefault());
    }

    /**
     * Parse a W3C-compliant tracestate header value, and return a {@link TraceState} with the details, or else
     * {@link TraceState#getDefault()}. <b>NOTE:</b> It's worth realizing that tracestate doesn't HAVE TO come from
     * a HTTP request, if your software doesn't listen for HTTP requests (or has a different use case).
     *
     * @param traceStateHeader The string containing tracestate information
     * @return The {@link TraceState} parsed, or else {@link TraceState#getDefault()}
     */
    public static TraceState extractTraceState(String traceStateHeader) {
        TraceStateBuilder traceStateBuilder = TraceState.builder();
        String[] listMembers = TRACESTATE_ENTRY_DELIMITER_SPLIT_PATTERN.split(traceStateHeader);
        checkArgument(listMembers.length <= TRACESTATE_MAX_MEMBERS, "TraceState has too many elements.");
        // Iterate in reverse order because when call builder set the elements is added in the
        // front of the list.
        for (int i = listMembers.length - 1; i >= 0; i--) {
            String listMember = listMembers[i];
            int index = listMember.indexOf(TRACESTATE_KEY_VALUE_DELIMITER);
            checkArgument(index != -1, "Invalid TraceState list-member format.");
            traceStateBuilder.put(listMember.substring(0, index), listMember.substring(index + 1));
        }
        TraceState traceState = traceStateBuilder.build();
        if (traceState.size() != listMembers.length) {
            // Validation failure, drop the tracestate
            return TraceState.getDefault();
        }
        return traceState;
    }
}
