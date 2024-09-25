package com.example.szakdolgozat.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class VideoService {

    private static final String UPLOAD_DIR = "./uploads/";

    public List<String> listAllVideos() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(UPLOAD_DIR))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }
}
