/*
 * Copyright Red Hat
 * SPDX-License-Identifier: Apache-2.0
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
                implements ConfigurablePropagatorProvider
{
    /**
     * Return the propagator instance
     * @param configProperties Not used
     * @return The {@link EnvarExtractingPropagator} instance
     */
    @Override
    public TextMapPropagator getPropagator( ConfigProperties configProperties )
    {
        return EnvarExtractingPropagator.getInstance();
    }

    /**
     * Return the keyword used for autoconfiguring this context propagator.
     * @return 'envar'
     */
    @Override
    public String getName()
    {
        return "envar";
    }
}
