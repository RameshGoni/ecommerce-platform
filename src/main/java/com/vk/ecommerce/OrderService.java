
package com.vk.ecommerce;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vk.ecommerce.entity.CartItem;
import com.vk.ecommerce.entity.Order;
import com.vk.ecommerce.entity.OrderItem;
import com.vk.ecommerce.entity.Product;
import com.vk.ecommerce.repository.CartItemRepository;
import com.vk.ecommerce.repository.OrderItemRepository;
import com.vk.ecommerce.repository.OrderRepository;
import com.vk.ecommerce.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

  
    @Transactional
    public Order placeOrder(Long userId) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName());
            }

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            totalAmount = totalAmount.add(itemTotal);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setOrderDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItemRepository.save(orderItem);

            product.setStockQuantity(
                    product.getStockQuantity() - cartItem.getQuantity());

            productRepository.save(product);

            cartItemRepository.delete(cartItem);
        }

        return savedOrder;
    }
   
    @Transactional
    public Order cancelOrder(Long orderId) {

    	Order order = orderRepository.findById(orderId)
    	        .orElseThrow(() -> new ResponseStatusException(
    	                HttpStatus.NOT_FOUND, "Order not found"));

        if (!"PLACED".equals(order.getStatus())) {
            throw new IllegalArgumentException(
                    "Only orders with PLACED status can be cancelled"
            );
        }

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(orderId);

        for (OrderItem orderItem : orderItems) {

            Product product = productRepository.findById(
                    orderItem.getProductId()
            ).orElseThrow(() -> new RuntimeException("Product not found"));

            product.setStockQuantity(
                    product.getStockQuantity() + orderItem.getQuantity()
            );

            productRepository.save(product);
        }

        order.setStatus("CANCELLED");

        return orderRepository.save(order);
    }
    
}
