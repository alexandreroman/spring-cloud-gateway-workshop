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

package fr.alexandreroman.demos.scgworkshop.routing.calc;

import lombok.extern.slf4j.Slf4j;
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
class CalcController {
    @GetMapping(value = "/api/calc/add", produces = MediaType.TEXT_PLAIN_VALUE)
    String add(@RequestParam(value = "a") int a, @RequestParam("b") int b) {
        log.info("Service calc/add: a={}, b={}", a, b);
        return String.valueOf(a + b);
    }

    @GetMapping(value = "/api/calc/subtract", produces = MediaType.TEXT_PLAIN_VALUE)
    String subtract(@RequestParam(value = "a") int a, @RequestParam("b") int b) {
        log.info("Service calc/subtract: a={}, b={}", a, b);
        return String.valueOf(a - b);
    }
}
