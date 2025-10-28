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

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

/**
 * {@link ConfigurablePropagatorProvider} to allow autoconfiguration with the envar propagator. This allows us to use
 * 'envar' in Quarkus application.yaml.
 */
public class EnvarExtractingConfigurablePropagator
        implements ConfigurablePropagatorProvider {
    /**
     * Return the propagator instance
     * 
     * @param configProperties Not used
     * @return The {@link EnvarExtractingPropagator} instance
     */
    @Override
    public TextMapPropagator getPropagator(ConfigProperties configProperties) {
        return EnvarExtractingPropagator.getInstance();
    }

    /**
     * Return the keyword used for autoconfiguring this context propagator.
     * 
     * @return 'envar'
     */
    @Override
    public String getName() {
        return "envar";
    }
}
