/*
 * Copyright (c) 2022 VMware, Inc. or its affiliates
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

package fr.alexandreroman.demos.scgworkshop.routing.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Profile;
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
        WireMockServer service = new WireMockServer(8082);
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
    @Profile("!v2")
    void testAdder() {
        WireMockServer service = new WireMockServer(8081);
        try {
            service.start();
            service.stubFor(get("/api/adder?a=3&b=4").willReturn(aResponse().withBody("7")));
            assertThat(client.getForEntity("/api/adder?a=3&b=4", String.class).getBody()).isEqualTo("7");
        } finally {
            service.stop();
        }
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "v2"})
class ApplicationV2Tests {
    @Autowired
    private TestRestTemplate client;

    @Test
    void contextLoads() {
    }

    @Test
    void testAdderV2() {
        WireMockServer service = new WireMockServer(8083);
        try {
            service.start();
            service.stubFor(get("/api/calc/add?a=3&b=4").willReturn(aResponse().withBody("7")));
            assertThat(client.getForEntity("/api/adder?a=3&b=4", String.class).getBody()).isEqualTo("7");
        } finally {
            service.stop();
        }
    }

    @Test
    void testCalc() {
        WireMockServer service = new WireMockServer(8083);
        try {
            service.start();
            service.stubFor(get("/api/calc/add?a=3&b=4").willReturn(aResponse().withBody("7")));
            assertThat(client.getForEntity("/api/calc/add?a=3&b=4", String.class).getBody())
                    .isEqualTo("7");
        } finally {
            service.stop();
        }
    }
}
