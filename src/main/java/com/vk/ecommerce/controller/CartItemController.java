
package com.vk.ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vk.ecommerce.entity.CartItem;
import com.vk.ecommerce.entity.User;
import com.vk.ecommerce.repository.UserRepository;
import com.vk.ecommerce.service.CartItemService;

@RestController
@RequestMapping("/api/cart")
public class CartItemController {

    private final CartItemService cartItemService;
    private final UserRepository userRepository;

    public CartItemController(
            CartItemService cartItemService,
            UserRepository userRepository) {
        this.cartItemService = cartItemService;
        this.userRepository = userRepository;
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found"));

        return user.getId();
    }

    @PostMapping
    public CartItem addToCart(
            @RequestBody CartItem cartItem,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);
        cartItem.setUserId(userId);

        return cartItemService.addToCart(cartItem, userId);
    }

    @GetMapping("/{userId}")
    public List<CartItem> getCartItems(
            @PathVariable Long userId,
            Authentication authentication) {

        Long authenticatedUserId =
                getAuthenticatedUserId(authentication);

        return cartItemService.getCartItems(
                userId, authenticatedUserId);
    }

    @DeleteMapping("/{id}")
    public String removeFromCart(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        cartItemService.removeFromCart(id, userId);

        return "Cart item removed successfully";
    }

    @PutMapping("/{id}")
    public CartItem updateQuantity(
            @PathVariable Long id,
            @RequestBody CartItem cartItem,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        return cartItemService.updateQuantity(
                id, cartItem.getQuantity(), userId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(
            IllegalArgumentException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleRuntimeException(RuntimeException ex) {
        return ex.getMessage();
    }
}
