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

package fr.alexandreroman.demos.scgworkshop.resiliency.greetings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
@Slf4j
class GreetingsController {
    @Value("${app.message.fallback:Hello world!}")
    private String messageFallback;
    @Value("${app.message.pattern:Hello %s!}")
    private String messagePattern;

    @GetMapping(value = "/api/greetings", produces = MediaType.TEXT_PLAIN_VALUE)
    String greetings(@RequestParam(value = "name", required = false) String name) {
        log.info("Service greetings: name={}", name);
        return doGreetings(name);
    }

    @GetMapping(value = "/api/greetings-slow", produces = MediaType.TEXT_PLAIN_VALUE)
    String slowGreetings(@RequestParam(value = "name", required = false) String name) throws InterruptedException {
        Thread.sleep(3000);
        log.info("Service greetings-slow: name={}", name);
        return doGreetings(name);
    }

    private String doGreetings(String name) {
        return name == null ? messageFallback : String.format(messagePattern, name);
    }
}
