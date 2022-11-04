package com.redhat.resilience.otel;

import com.redhat.resilience.otel.fixture.CustomTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile( CustomTestProfile.class )
public class TraceparentEnvarQuarkusTest
{
    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String SPAN_ID = "b9c7c989f97918e1";

    @Test
    public void test( TestInfo testInfo )
    {
        System.out.println( testInfo.getDisplayName() );

        given().when().get( "/test" ).then().statusCode( 200 );
        Response response = given().when().get( "/spans" ).thenReturn();
        String[] traceLines = response.getBody().print().split( "\n" );
        System.out.println(traceLines.length + " spans");

        for ( String line : traceLines )
        {
            String[] parts = line.split( "," );
            assertEquals( TRACE_ID, parts[0], "Incorrect trace ID!" );
        }
    }
}
