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

package fr.alexandreroman.demos.scgworkshop.monolith.hrapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/people")
public class PeopleResource {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<Person> people;

    public PeopleResource() {
        people = List.of(
                new Person(1L, "Steve", "Rogers"),
                new Person(2L, "Tony", "Stark")
        );
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPeople() {
        logger.info("Getting people from JavaEE app");
        return Response.ok(people)
                .header("X-Source", "JavaEE")
                .build();
    }
}
