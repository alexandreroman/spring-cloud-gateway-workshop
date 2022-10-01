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

package fr.alexandreroman.demos.scgworkshop.resiliency.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
@Slf4j
class TimeController {
    @Value("${app.version:1}")
    private int appVersion;

    @GetMapping(value = "/api/time", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> time() {
        log.info("Service time: appVersion={}", appVersion);
        return ResponseEntity.status(HttpStatus.OK)
                .header("X-App-Version", String.valueOf(appVersion))
                .body("V" + appVersion + ": " + doTime());
    }

    private String doTime() {
        if (appVersion == 1) {
            return DateTimeFormatter.RFC_1123_DATE_TIME
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
        }
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }
}
