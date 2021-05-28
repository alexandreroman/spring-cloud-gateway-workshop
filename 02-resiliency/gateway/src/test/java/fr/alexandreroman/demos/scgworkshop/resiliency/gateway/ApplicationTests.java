/*
 * Copyright (c) 2021 VMware, Inc. or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.demos.scgworkshop.resiliency.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationTests {
    @Autowired
    private TestRestTemplate client;

    @Test
    void contextLoads() {
    }

    @Test
    void testGreetings() {
        WireMockServer service = new WireMockServer(12000);
        try {
            service.start();

            service.stubFor(get("/api/greetings").willReturn(aResponse().withBody("Hello world!")));
            service.stubFor(get("/api/greetings?name=Alex").willReturn(aResponse().withBody("Hello Alex!")));

            assertThat(client.getForEntity("/api/greetings", String.class).getBody()).isEqualTo("Hello world!");
            assertThat(client.getForEntity("/api/greetings?name=Alex", String.class).getBody()).isEqualTo("Hello Alex!");
        } finally {
            service.stop();
        }
    }

    @Test
    void testGreetingsCircuitBreaker() {
        final var msg = "Greetings service is not available :(";
        assertThat(client.getForEntity("/api/greetings", String.class).getBody()).isEqualTo(msg);
        assertThat(client.getForEntity("/api/greetings?name=Alex", String.class).getBody()).isEqualTo(msg);
    }

    @Test
    void testGreetingsSlow() {
        WireMockServer service = new WireMockServer(12000);
        try {
            service.start();
            service.stubFor(get("/api/greetings-slow").willReturn(aResponse().withFixedDelay(3000).withBody("Hello world!")));
            assertThat(client.getForEntity("/api/greetings-slow", String.class).getBody()).isEqualTo("Greetings service is too slow");
        } finally {
            service.stop();
        }
    }

    @Test
    void testTime() {
        WireMockServer time1 = new WireMockServer(12001);
        WireMockServer time2 = new WireMockServer(12002);
        try {
            time1.start();
            time2.start();
            time1.stubFor(get("/api/time").willReturn(aResponse().withBody("V1")));
            time2.stubFor(get("/api/time").willReturn(aResponse().withBody("V2")));

            int nbV1 = 0;
            int nbV2 = 0;
            for (int i = 0; i < 100; ++i) {
                final var resp = client.getForEntity("/api/time", String.class);
                assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resp.getBody()).isIn("V1", "V2");
                if (resp.getBody().equals("V1")) {
                    nbV1 += 1;
                }
                if (resp.getBody().equals("V2")) {
                    nbV2 += 1;
                }
            }
            assertThat(nbV1).isBetween(85, 100);
            assertThat(nbV2).isBetween(0, 15);
        } finally {
            time1.stop();
            time2.stop();
        }
    }
}
