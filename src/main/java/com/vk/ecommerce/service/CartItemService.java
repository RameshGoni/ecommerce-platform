package com.vk.ecommerce.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vk.ecommerce.entity.CartItem;
import com.vk.ecommerce.repository.CartItemRepository;
import com.vk.ecommerce.repository.UserRepository;
import com.vk.ecommerce.repository.ProductRepository;

@Service
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartItemService(
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public CartItem addToCart(CartItem cartItem) {

        if (cartItem.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero");
        }

        if (cartItem.getUserId() == null
                || !userRepository.existsById(cartItem.getUserId())) {
            throw new IllegalArgumentException("User not found");
        }

        if (cartItem.getProductId() == null
                || !productRepository.existsById(cartItem.getProductId())) {
            throw new IllegalArgumentException("Product not found");
        }

        return cartItemRepository
                .findByUserIdAndProductId(
                        cartItem.getUserId(),
                        cartItem.getProductId())
                .map(existingItem -> {
                    existingItem.setQuantity(
                            existingItem.getQuantity()
                                    + cartItem.getQuantity());
                    return cartItemRepository.save(existingItem);
                })
                .orElseGet(() -> cartItemRepository.save(cartItem));
    }

    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    public CartItem updateQuantity(Long id, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero");
        }

        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Cart item not found"));

        cartItem.setQuantity(quantity);

        return cartItemRepository.save(cartItem);
    }

    public void removeFromCart(Long id) {

        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Cart item not found"));

        cartItemRepository.delete(cartItem);
    }

public CartItem addToCart(CartItem cartItem, Long authenticatedUserId) {
    if (cartItem.getUserId() == null
            || !cartItem.getUserId().equals(authenticatedUserId)) {
        throw new IllegalArgumentException(
                "You can only add items to your own cart");
    }

    return addToCart(cartItem);
}

public List<CartItem> getCartItems(
        Long userId, Long authenticatedUserId) {
    if (!userId.equals(authenticatedUserId)) {
        throw new IllegalArgumentException(
                "You can only view your own cart");
    }

    return getCartItems(userId);
}

public CartItem updateQuantity(
        Long id, int quantity, Long authenticatedUserId) {
    CartItem item = cartItemRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Cart item not found"));

    if (!item.getUserId().equals(authenticatedUserId)) {
        throw new IllegalArgumentException(
                "You can only update your own cart items");
    }

    return updateQuantity(id, quantity);
}

public void removeFromCart(Long id, Long authenticatedUserId) {
    CartItem item = cartItemRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Cart item not found"));

    if (!item.getUserId().equals(authenticatedUserId)) {
        throw new IllegalArgumentException(
                "You can only remove your own cart items");
    }

    removeFromCart(id);
}

}