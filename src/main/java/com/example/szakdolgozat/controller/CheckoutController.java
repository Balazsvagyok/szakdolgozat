package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.model.Cart;
import com.example.szakdolgozat.model.CartItem;
import com.example.szakdolgozat.model.Purchase;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.CartRepository;
import com.example.szakdolgozat.repository.PurchaseRepository;
import com.example.szakdolgozat.service.CartService;
import com.example.szakdolgozat.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

@Controller
public class CheckoutController {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private CartService cartService;

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return userPrincipal.getUser();
    }

    public double calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }


    @GetMapping("/checkout")
    public String checkoutPage(Model model) {
        User user = getLoggedInUser();
        Cart cart = cartRepository.findByUser(user);
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cartRepository.save(cart);
        }
        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        model.addAttribute("role", user.getRole());
        model.addAttribute("cartService", cartService);
        model.addAttribute("total", calculateTotal(cart));
        return "checkout";
    }

    @PostMapping("/checkout/submit")
    public String checkout(RedirectAttributes redirectAttributes) {
        User user = getLoggedInUser();
        Cart cart = cartRepository.findByUser(user);

        for (CartItem item : cart.getItems()) {
            Purchase purchase = new Purchase();
            purchase.setUser(user);
            purchase.setFile(item.getFile());
            purchase.setPurchaseDate(new Date());
            purchaseRepository.save(purchase);
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        redirectAttributes.addFlashAttribute("message", "Sikeresen fizetve");
        return "redirect:/";
    }
}

