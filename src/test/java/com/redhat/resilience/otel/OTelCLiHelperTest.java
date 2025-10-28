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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.redhat.resilience.otel.fixture.TestSpanExporter;
import com.redhat.resilience.otel.internal.EnvarExtractingPropagator;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class OTelCLiHelperTest {
    static {
        System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    @AfterEach
    public void otelTeardown() {
        OTelCLIHelper.stopOTel();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    public void verifyDoubleStart() {
        OTelCLIHelper.startOTel("cli-test", SimpleSpanProcessor.create(new TestSpanExporter()));

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> OTelCLIHelper.startOTel(
                        "traceparent-envar-cli-test",
                        SimpleSpanProcessor.create(new TestSpanExporter())));

        assertTrue(thrown.getMessage().contains("startOTel has already been called"));
    }

    @Test
    public void verifyDoubleStop() {
        OTelCLIHelper.startOTel("cli-test", SimpleSpanProcessor.create(new TestSpanExporter()));
        OTelCLIHelper.stopOTel();
        OTelCLIHelper.stopOTel();
    }

    @Test
    public void testParseUrlWithFile(@TempDir Path tempDir)
            throws IOException {
        Path numbers = tempDir.resolve("testFile.txt");
        Files.write(numbers, Collections.singletonList("content"));
        assertEquals("content", EnvarExtractingPropagator.parseURL(numbers.toFile().toURI().toString()));
    }

    @Test
    public void testParseUrlWithURL() {
        assertFalse(EnvarExtractingPropagator.parseURL("http://www.google.com").isEmpty());
    }

    @Test
    public void testParseUrlWithTraceID() {
        String trace = "0af7651916cd43dd8448eb211c80319c";
        assertEquals(EnvarExtractingPropagator.parseURL(trace), trace);
    }
}
