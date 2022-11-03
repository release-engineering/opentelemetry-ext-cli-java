package com.redhat.resilience.otel.fixture;

import io.opentelemetry.sdk.trace.data.SpanData;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;

public class QuarkusTestResource
{
    @Path("/test")
    @HEAD
    public Response head()
    {
        return Response.ok().build();
    }

    @Path("/spans")
    @GET
    public List<SpanData> get()
    {
        return TestSpanExporter.getSpans();
    }
}
