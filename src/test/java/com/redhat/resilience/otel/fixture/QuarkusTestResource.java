package com.redhat.resilience.otel.fixture;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.ReadableSpan;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Path("/test")
public class QuarkusTestResource
{
    @GET
    public Response test()
    {
        Span span = Span.current();

        Span client = GlobalOpenTelemetry.get()
                                  .getTracer( "test-client" )
                                  .spanBuilder( "get-test-resource" )
                                  .setSpanKind( SpanKind.CLIENT )
                                  .setAttribute( "service.name", "sidecar" )
                                  .startSpan();


        client.end();

        TestSpanExporter.record(
                        Arrays.asList( ( (ReadableSpan) span ).toSpanData(), ( (ReadableSpan) client ).toSpanData() ) );

        return Response.ok().build();
    }
}
