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

package fr.alexandreroman.demos.scgworkshop.monolith.hrapp.people

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

data class Person(val id: Long, val firstName: String, val lastName: String)

@RestController
class PeopleController {
    private val people: List<Person> = createSampleData()
    private val hostName: String = InetAddress.getLocalHost().canonicalHostName
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/api/people")
    fun getPeople(): ResponseEntity<Any> {
        logger.info("Getting people from Spring Boot app")
        return ResponseEntity.ok()
            .header("X-Source", "SpringBoot")
            .header("X-Host", hostName)
            .body(people)
    }
}

private fun createSampleData() = listOf<Person>(
    Person(1L, "Steve", "Rogers"),
    Person(2L, "Tony", "Stark")
)
