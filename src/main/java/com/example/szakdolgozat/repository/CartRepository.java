package com.example.szakdolgozat.repository;

import com.example.szakdolgozat.model.Cart;
import com.example.szakdolgozat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByUser(User user);
}
