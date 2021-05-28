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

package fr.alexandreroman.demos.scgworkshop.security.items;

import lombok.Data;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Arrays;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @ConditionalOnProperty(value = "app.data.inject", havingValue = "true", matchIfMissing = true)
    CommandLineRunner initData(ItemRepository repo) {
        return (args) -> {
            final var item1 = new Item();
            item1.setName("LEGO Star Wars - Millenium Falcon");
            item1.setPrice(BigDecimal.valueOf(200));

            final var item2 = new Item();
            item2.setName("LEGO Star Wars - Death Star");
            item2.setPrice(BigDecimal.valueOf(500));

            final var item3 = new Item();
            item3.setName("LEGO Star Wars - Tie Fighter");
            item3.setPrice(BigDecimal.valueOf(100));

            repo.saveAll(Arrays.asList(item1, item2, item3));
        };
    }
}

@Data
@Entity
class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private BigDecimal price;
    @Version
    private Long version;
}

@RepositoryRestResource(path = "items")
interface ItemRepository extends JpaRepository<Item, Long> {
}
