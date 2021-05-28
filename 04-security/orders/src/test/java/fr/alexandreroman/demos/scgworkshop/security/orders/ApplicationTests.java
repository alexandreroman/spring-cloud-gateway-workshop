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

package fr.alexandreroman.demos.scgworkshop.security.orders;

import com.github.tomakehurst.wiremock.common.Json;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 12345)
@Import({TestConfig.class, TestSecurityConfig.class})
class ApplicationTests {
    @Autowired
    private TestRestTemplate client;
    @LocalServerPort
    private int port;
    @MockBean
    private AuthenticationContext ctx;

    @Test
    void contextLoads() {
    }

    @Test
    void testWorkflow() throws URISyntaxException {
        given(ctx.getUser()).willReturn("test");
        given(ctx.isAdmin()).willReturn(false);

        var resp = client.postForEntity("/api/orders", null, OrderDTO.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var order = resp.getBody();
        assertThat(order.getId()).isEqualTo(1);
        assertThat(order.getCustomer()).isEqualTo("test");
        assertThat(order.getDetails()).isEmpty();

        order = client.getForEntity("/api/orders/1", OrderDTO.class).getBody();
        assertThat(order.getId()).isEqualTo(1);
        assertThat(order.getCustomer()).isEqualTo("test");
        assertThat(order.getDetails()).isEmpty();

        var itemPriceJson = Json.write(new ItemPrice(BigDecimal.valueOf(12)));
        stubFor(get(urlEqualTo("/api/items/2")).willReturn(okJson(itemPriceJson)));

        order = client.postForEntity("/api/orders/1/items/2", new ItemQuantity(3), OrderDTO.class).getBody();
        assertThat(order.getId()).isEqualTo(1);
        assertThat(order.getCustomer()).isEqualTo("test");
        assertThat(order.getDetails()).hasSize(1);
        assertThat(order.getDetails().get(0)).isEqualTo(new OrderDTO.DetailDTO(2, 3, BigDecimal.valueOf(12), "/api/items/2"));

        client.delete("/api/orders/1/items/2");
        order = client.getForEntity("/api/orders/1", OrderDTO.class).getBody();
        assertThat(order.getId()).isEqualTo(1);
        assertThat(order.getCustomer()).isEqualTo("test");
        assertThat(order.getDetails()).isEmpty();

        client.delete("/api/orders/1");
        assertThat(client.getForEntity("/api/orders/1", OrderDTO.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

@TestConfiguration
class TestConfig {
    @Autowired
    private AuthenticationContext ctx;

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    AuthenticationContextResolver authenticationContextResolver() {
        return new AuthenticationContextResolver() {
            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
                return ctx;
            }
        };
    }
}

