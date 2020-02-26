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

package fr.alexandreroman.demos.scgworkshop.security.orders;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toUnmodifiableList;

@SpringBootApplication
@EnableJpaRepositories
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.data", name = "inject", havingValue = "true")
    CommandLineRunner initData(OrderService os) {
        return (args) -> {
            final var ctx = new AuthenticationContext("_INIT_", true);
            final var order = os.createOrder(ctx, "han");
            os.addItemToOrder(ctx, order.getId(), 1, 1);
            os.addItemToOrder(ctx, order.getId(), 2, 1);
        };
    }
}

@RestController
@RequiredArgsConstructor
class DataInjectorController {
    private final OrderService os;
    private final Random random = new Random();

    @GetMapping("/api/orders/inject")
    boolean injectData(AuthenticationContext ctx) {
        final var order = os.createOrder(ctx, ctx.getUser());
        os.addItemToOrder(ctx, order.getId(), 1, random.nextInt(3));
        os.addItemToOrder(ctx, order.getId(), 2, random.nextInt(5));
        return true;
    }
}

@RestController
@RequiredArgsConstructor
class OrderController {
    private final OrderService os;

    @PostMapping("/api/orders")
    ResponseEntity<OrderDTO> createOrder(AuthenticationContext ctx) {
        final var result = os.createOrder(ctx, ctx.getUser());
        return ResponseEntity.created(getOrderUri(result.getId(), ctx)).body(result);
    }

    @DeleteMapping("/api/orders/{orderId}")
    void deleteOrder(@PathVariable long orderId, AuthenticationContext ctx) {
        os.deleteOrder(ctx, orderId);
    }

    @GetMapping("/api/orders/{orderId}")
    ResponseEntity<?> getOrder(@PathVariable long orderId, AuthenticationContext ctx) {
        final var order = os.getOrder(ctx, orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/api/orders/{orderId}/items/{itemId}")
    ResponseEntity<OrderDTO> addItemToOrder(@PathVariable long orderId,
                                            @PathVariable long itemId,
                                            @Valid @RequestBody ItemQuantity iq,
                                            AuthenticationContext ctx) {
        final OrderDTO result = os.addItemToOrder(ctx, orderId, itemId, iq.getQuantity());
        return ResponseEntity.status(HttpStatus.OK)
                .location(getOrderUri(orderId, ctx))
                .body(result);
    }

    @DeleteMapping("/api/orders/{orderId}/items/{itemId}")
    void deleteItemFromOrder(@PathVariable long orderId,
                             @PathVariable long itemId,
                             AuthenticationContext ctx) {
        os.deleteItemFromOrder(ctx, orderId, itemId);
    }

    private static URI getOrderUri(long orderId, AuthenticationContext ctx) {
        return MvcUriComponentsBuilder.fromMethodName(OrderController.class, "getOrder", orderId, ctx)
                .build().toUri();
    }
}

@ControllerAdvice
class ControllerExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> onNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<?> onEntityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class OrderDTO {
    private long id;
    private String customer;
    private List<DetailDTO> details = List.of();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class DetailDTO {
        private long id;
        private int quantity;
        private BigDecimal price;
        private String href;

        DetailDTO(final OrderDetail detail) {
            this.id = detail.getItemId();
            this.quantity = detail.getQuantity();
            this.price = detail.getPrice();
            this.href = "/api/items/" + detail.getItemId();
        }
    }

    OrderDTO(final Order order, final Iterable<OrderDetail> details) {
        this.id = order.getId();
        this.customer = order.getCustomer();
        this.details = StreamSupport.stream(details.spliterator(), false)
                .map(d -> new DetailDTO(d)).collect(toUnmodifiableList());
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
class OrderService {
    private final OrderRepository orderRepo;
    private final OrderDetailRepository orderDetailRepo;
    private final RestTemplate client;
    @Value("${app.services.items}")
    private String itemsUri;

    @Transactional
    OrderDTO createOrder(AuthenticationContext ctx, @NonNull String customer) {
        Order order = new Order();
        order.setCustomer(customer);
        order = orderRepo.save(order);

        log.info("Created order {} for customer {}",
                order.getId(), customer);

        return new OrderDTO(order, List.of());
    }

    @Transactional
    void deleteOrder(AuthenticationContext ctx, long id) {
        final var opt = orderRepo.findById(id);
        if (!opt.isPresent()) {
            return;
        }
        final var order = opt.get();
        if (!ctx.getUser().equals(order.getCustomer()) && !ctx.isAdmin()) {
            throw new AccessDeniedException();
        }
        orderRepo.deleteById(id);
        log.info("Deleted order {}", id);
    }

    @Transactional
    OrderDTO addItemToOrder(AuthenticationContext ctx, long orderId, long itemId, int quantity) {
        final var order = orderRepo.findById(orderId).get();
        if (!ctx.getUser().equals(order.getCustomer()) && !ctx.isAdmin()) {
            throw new AccessDeniedException();
        }

        boolean itemFound = false;
        for (final OrderDetail detail : orderDetailRepo.findByOrderAndItemId(order, itemId)) {
            itemFound = true;
            if (quantity < 1) {
                log.info("Deleting item {} from order {}", itemId, orderId);
                orderDetailRepo.delete(detail);
            } else {
                log.info("Updating item {} in order {} (quantity: {})",
                        itemId, orderId, quantity);
                detail.setQuantity(quantity);
                detail.setPrice(fetchItemPrice(itemId));
                orderDetailRepo.save(detail);
            }
        }
        if (!itemFound && quantity > 0) {
            final OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setItemId(itemId);
            detail.setQuantity(quantity);
            detail.setPrice(fetchItemPrice(itemId));
            log.info("Adding item {} to order {} (quantity: {})", itemId, orderId, quantity);
            orderDetailRepo.save(detail);
        }
        return getOrder(ctx, orderId);
    }

    private BigDecimal fetchItemPrice(long itemId) {
        log.info("Fetching price for item {}", itemId);
        final String itemPriceUri = UriComponentsBuilder.fromUriString(itemsUri)
                .path("/api/items/{itemId}").buildAndExpand(itemId).toUriString();
        return client.getForEntity(itemPriceUri, ItemPrice.class).getBody().getPrice();
    }

    @Transactional
    void deleteItemFromOrder(AuthenticationContext ctx, long orderId, long itemId) {
        final var order = orderRepo.findById(orderId).get();
        if (!ctx.getUser().equals(order.getCustomer()) && !ctx.isAdmin()) {
            throw new AccessDeniedException();
        }

        log.info("Deleting item {} from order {}", itemId, orderId);
        orderDetailRepo.deleteFromOrder(orderId, itemId);
    }

    @Transactional(readOnly = true)
    OrderDTO getOrder(AuthenticationContext ctx, long orderId) {
        final var order = orderRepo.findById(orderId).get();
        if (!ctx.getUser().equals(order.getCustomer()) && !ctx.isAdmin()) {
            throw new AccessDeniedException();
        }

        final var details = orderDetailRepo.findByOrder(order);
        final List<OrderDetail> listDetails = new ArrayList<>(4);
        details.forEach(listDetails::add);
        return new OrderDTO(order, listDetails);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ItemPrice {
    private BigDecimal price;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ItemQuantity {
    @NotNull
    private Integer quantity;
}

@Data
@Entity
@Table(name = "CUSTOMER_ORDERS")
class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String customer;
}

@Data
@Entity
@Table(name = "CUSTOMER_ORDER_DETAILS")
class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(nullable = false)
    private Order order;
    @Column(nullable = false)
    private Long itemId;
    @Column(nullable = false)
    private Integer quantity = 0;
    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;
}

@Repository
interface OrderRepository extends JpaRepository<Order, Long> {
}

@Repository
interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    Iterable<OrderDetail> findByOrderAndItemId(Order order, long itemId);

    Iterable<OrderDetail> findByOrder(Order order);

    @Modifying
    @Query("DELETE from OrderDetail detail where detail.order.id=:orderId AND detail.itemId=:itemId")
    @Transactional
    void deleteFromOrder(long orderId, long itemId);
}
