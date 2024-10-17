package com.example.szakdolgozat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.szakdolgozat.model.User;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("SELECT u FROM User u WHERE u.username = ?1")
    User findByUsername(String username);

    List<User> findByUsernameContaining(String username);
}