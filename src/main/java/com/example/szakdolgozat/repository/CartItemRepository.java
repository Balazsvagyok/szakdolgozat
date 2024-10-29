package com.example.szakdolgozat.repository;

import com.example.szakdolgozat.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
