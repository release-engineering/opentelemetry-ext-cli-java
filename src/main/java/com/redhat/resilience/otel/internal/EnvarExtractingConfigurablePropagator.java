/*
 * Copyright Red Hat
 * SPDX-License-Identifier: Apache-2.0
 */

package com.redhat.resilience.otel.internal;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

public class EnvarExtractingConfigurablePropagator
                implements ConfigurablePropagatorProvider
{
    @Override
    public TextMapPropagator getPropagator( ConfigProperties configProperties )
    {
        return EnvarExtractingPropagator.getInstance();
    }

    @Override
    public String getName()
    {
        return "envar";
    }
}
