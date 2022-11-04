package com.redhat.resilience.otel.fixture;

import io.opentelemetry.sdk.trace.data.SpanData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path( "/spans" )
public class QuarkusSpansResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @GET
    public String get()
    {
        StringBuilder sb = new StringBuilder();
        List<SpanData> spans = TestSpanExporter.getSpans();
        logger.info( "Got {} spans:\n{}", spans.size(), spans );

        spans.forEach( spanData -> {
            if ( sb.length() > 0)
            {
                sb.append( "\n" );
            }

            sb.append( spanData.getTraceId() )
              .append( ',' )
              .append( spanData.getParentSpanId() )
              .append( ',' )
              .append( spanData.getSpanId() )
              .append( ',' )
              .append( spanData.getName() )
              .append( ',' )
              .append( spanData.getKind().name() );
        } );

//        logger.info( "In QuarkusSpansResource, returning spans string:\n {}", sb );
        return sb.toString();
    }
}
