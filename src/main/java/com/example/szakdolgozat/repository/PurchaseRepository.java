package com.example.szakdolgozat.repository;

import com.example.szakdolgozat.model.Purchase;
import com.example.szakdolgozat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
    List<Purchase> findByUser(User user);

    long countByUser(User user);
}

