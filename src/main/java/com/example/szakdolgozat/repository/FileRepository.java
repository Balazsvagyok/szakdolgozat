package com.example.szakdolgozat.repository;

import com.example.szakdolgozat.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, String> {

    Optional<File> findByName(String name);
}
