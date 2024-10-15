package com.example.szakdolgozat.repository;

import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.Purchase;
import com.example.szakdolgozat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
    List<Purchase> findByUser(User user);
    long countByUser(User user);
    Optional<Purchase> findByUserAndFile(User user, File file);
    boolean existsByUserAndFile(User user, File file);
}

