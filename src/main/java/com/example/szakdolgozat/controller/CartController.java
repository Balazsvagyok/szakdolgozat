package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.model.Cart;
import com.example.szakdolgozat.model.CartItem;
import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.CartItemRepository;
import com.example.szakdolgozat.repository.CartRepository;
import com.example.szakdolgozat.repository.FileRepository;
import com.example.szakdolgozat.repository.UserRepository;
import com.example.szakdolgozat.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    private User getLoggedInUser(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return userPrincipal.getUser();
    }

    // Kosár tartalmának megtekintése
    @GetMapping
    public String viewCart(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getLoggedInUser(authentication);
        Cart cart = cartRepository.findByUser(user);
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cartRepository.save(cart);
            return "redirect:/cart";
        }
        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        model.addAttribute("role", user.getRole());
        return "cart";
    }

    @PostMapping("/add/{fileId}")
    public String addToCart(@PathVariable Long fileId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getLoggedInUser(authentication);
        File file = fileRepository.findById(String.valueOf(fileId)).orElseThrow(() -> new RuntimeException("File not found"));
        Cart cart = cartRepository.findByUser(user);

        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cartRepository.save(cart);
        }

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setFile(file);
        cartItem.setQuantity(1);
        cartItem.setPrice(file.getPrice());
        cartItemRepository.save(cartItem);

        return "redirect:/cart";
    }

    @PostMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId) {
        cartItemRepository.deleteById(itemId);
        return "redirect:/cart";
    }
}

