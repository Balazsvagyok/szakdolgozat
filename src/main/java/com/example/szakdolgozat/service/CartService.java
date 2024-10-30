package com.example.szakdolgozat.service;

import com.example.szakdolgozat.model.Cart;
import com.example.szakdolgozat.model.CartItem;
import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.CartItemRepository;
import com.example.szakdolgozat.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    public double calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
}

