package com.example.szakdolgozat.repository;

import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.Rating;
import com.example.szakdolgozat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByFile(File file);
    List<Rating> findByFileId(Long fileId);
    List<Rating> findByUserAndFile(User user, File file);
}

