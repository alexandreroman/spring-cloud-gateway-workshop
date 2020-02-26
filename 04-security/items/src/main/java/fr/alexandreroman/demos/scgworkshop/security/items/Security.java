/*
 * Copyright (c) 2020 VMware, Inc. or its affiliates
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

package fr.alexandreroman.demos.scgworkshop.security.items;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@Profile("!test")
class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        final String adminAuthority = "SCOPE_app.admin";
        http.oauth2ResourceServer().jwt();
        http.authorizeRequests()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                .mvcMatchers(HttpMethod.DELETE, "/api/**").hasAuthority(adminAuthority)
                .mvcMatchers(HttpMethod.POST, "/api/**").hasAuthority(adminAuthority)
                .mvcMatchers(HttpMethod.PUT, "/api/**").hasAuthority(adminAuthority)
                .mvcMatchers("/api/**/whoami").authenticated()
                .mvcMatchers("/api/**").permitAll();
    }
}
