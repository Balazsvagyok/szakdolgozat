package com.example.szakdolgozat.repository;

import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, String> {

    Optional<File> findByNameAndDeletedFalse(String name);

    List<File> findAllByDeletedFalse();

    List<File> findAllByUploader(User user);
}
