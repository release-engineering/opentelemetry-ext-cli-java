package com.redhat.resilience.otel;

import com.redhat.resilience.otel.fixture.CustomTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile( CustomTestProfile.class )
public class TraceparentEnvarQuarkusTest
{
    @Test
    public void test()
    {
        given().when().head( "/test" ).then().statusCode( 200 );
        Response response = given().when().get( "/spans" ).thenReturn();
        System.out.println( response.getBody().print() );
    }
}
