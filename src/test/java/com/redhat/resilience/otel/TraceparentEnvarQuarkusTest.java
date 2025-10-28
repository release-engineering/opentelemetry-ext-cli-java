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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.redhat.resilience.otel.fixture.CustomTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

@QuarkusTest
@TestProfile(CustomTestProfile.class)
public class TraceparentEnvarQuarkusTest {
    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String SPAN_ID = "b9c7c989f97918e1";

    private EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void test(TestInfo testInfo) throws Exception {
        System.out.println(testInfo.getDisplayName());

        environmentVariables.set("TRACEPARENT", "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01")
                .and("TRACESTATE", "rojo=00f067aa0ba902b7,congo=t61rcWkgMzE")
                .execute(() -> {
                    given().when().get("/test").then().statusCode(200);
                    Response response = given().when().get("/spans").thenReturn();
                    String[] traceLines = response.getBody().print().split("\n");
                    System.out.println(traceLines.length + " spans");

                    for (String line : traceLines) {
                        String[] parts = line.split(",");
                        assertEquals(TRACE_ID, parts[0], "Incorrect trace ID!");
                    }
                });
    }
}
