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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@EnableWebSecurity
@Profile("!test")
class SecurityConfig extends WebSecurityConfigurerAdapter {
    public static final String ADMIN_AUTHORITY = "SCOPE_app.admin";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer().jwt();
        http.authorizeRequests()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                .mvcMatchers(HttpMethod.DELETE, "/api/**").hasAuthority(ADMIN_AUTHORITY)
                .mvcMatchers(HttpMethod.POST, "/api/**").hasAuthority(ADMIN_AUTHORITY)
                .mvcMatchers(HttpMethod.PUT, "/api/**").hasAuthority(ADMIN_AUTHORITY)
                .mvcMatchers("/api/**/whoami").authenticated()
                .mvcMatchers("/api/**").permitAll();
    }
}

@Getter
@RequiredArgsConstructor
class AuthenticationContext {
    private final String user;
    private final boolean admin;
}

@Component
@Profile("!test")
class AuthenticationContextResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticationContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        final var jwt = (JwtAuthenticationToken) auth;
        final var user = jwt.getToken().getClaim("user_name").toString();
        final boolean admin = auth.getAuthorities()
                .stream().anyMatch(g -> SecurityConfig.ADMIN_AUTHORITY.equals(g.getAuthority()));
        return new AuthenticationContext(user, admin);
    }
}

@Configuration
@RequiredArgsConstructor
class WebSecurityConfig implements WebMvcConfigurer {
    private final AuthenticationContextResolver authContextResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authContextResolver);
    }
}

@ControllerAdvice
class AccessControllerAdvice {
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> onAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}

class AccessDeniedException extends RuntimeException {
    public AccessDeniedException() {
        super("Access denied to resource");
    }
}

@Configuration
@Slf4j
class RestTemplateConfig {
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    RestTemplateBuilder restTemplateBuilder() {
        final RestTemplateBuilder restBuilder = new RestTemplateBuilder();
        restBuilder.defaultHeader(HttpHeaders.USER_AGENT, applicationName);
        restBuilder.setConnectTimeout(Duration.ofSeconds(2));
        restBuilder.setReadTimeout(Duration.ofSeconds(2));

        final var authInterceptor = new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    log.trace("Not adding JWT authentication header since there is one already");
                    return execution.execute(request, body);
                }
                final var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null) {
                    log.trace("Not adding JWT authentication header since no user is currently authenticated");
                    return execution.execute(request, body);
                }

                final var jwt = (JwtAuthenticationToken) auth;
                // You may want to implement token refresh here.
                final var bearer = jwt.getToken().getTokenValue();

                log.debug("Adding JWT authentication header to outgoing request: {}", request.getURI());
                request.getHeaders().setBearerAuth(bearer);
                return execution.execute(request, body);
            }
        };
        restBuilder.additionalInterceptors(authInterceptor);

        return restBuilder;
    }

    @Bean
    @LoadBalanced
    @Profile("!test")
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
