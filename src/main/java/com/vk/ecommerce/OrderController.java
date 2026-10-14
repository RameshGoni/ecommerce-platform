
package com.vk.ecommerce;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.vk.ecommerce.entity.Order;
import com.vk.ecommerce.entity.OrderItem;
import com.vk.ecommerce.entity.User;
import com.vk.ecommerce.repository.OrderItemRepository;
import com.vk.ecommerce.repository.OrderRepository;
import com.vk.ecommerce.repository.UserRepository;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public OrderController(
            OrderService orderService,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository) {

        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        return user.getId();
    }

    private Order getOwnedOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only access your own orders");
        }

        return order;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Order> placeOrder(
            @PathVariable Long userId,
            Authentication authentication) {

        Long authenticatedUserId =
                getAuthenticatedUserId(authentication);

        if (!userId.equals(authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only place orders for yourself");
        }

        Order order = orderService.placeOrder(authenticatedUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(
            @PathVariable Long userId,
            Authentication authentication) {

        Long authenticatedUserId =
                getAuthenticatedUserId(authentication);

        if (!userId.equals(authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view your own orders");
        }

        return orderRepository.findByUserId(authenticatedUserId);
    }

    @GetMapping("/{orderId}/items")
    public List<OrderItem> getOrderItems(
            @PathVariable Long orderId,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);
        getOwnedOrder(orderId, userId);

        return orderItemRepository.findByOrderId(orderId);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);
        Order order = getOwnedOrder(orderId, userId);

        List<String> validStatuses = List.of(
                "PLACED", "SHIPPED", "DELIVERED", "CANCELLED");

        status = status.toUpperCase().trim();

        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid status. Allowed values: PLACED, SHIPPED, DELIVERED, CANCELLED");
        }

        String currentStatus = order.getStatus();

        boolean validTransition =
                ("PLACED".equals(currentStatus)
                        && List.of("SHIPPED", "CANCELLED").contains(status))
                || ("SHIPPED".equals(currentStatus)
                        && List.of("DELIVERED", "CANCELLED").contains(status));

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Cannot change order status from "
                            + currentStatus + " to " + status);
        }

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);
        getOwnedOrder(orderId, userId);

        Order cancelledOrder = orderService.cancelOrder(orderId);

        return ResponseEntity.ok(cancelledOrder);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            IllegalArgumentException ex) {

        Map<String, String> error = Map.of("error", ex.getMessage());

        return ResponseEntity.badRequest().body(error);
    }
}
