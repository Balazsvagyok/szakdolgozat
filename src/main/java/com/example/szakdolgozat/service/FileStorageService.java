package com.example.szakdolgozat.service;

import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    @Autowired
    private FileRepository fileRepository;

    public File store(MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        File File = new File(fileName, file.getContentType(), file.getBytes());

        return fileRepository.save(File);
    }

    public File saveFile(File file) throws IOException {
        return fileRepository.save(file);
    }

    public File getFile(String id) {
        return fileRepository.findById(id).get();
    }

    public Stream<File> getAllFiles() {
        return fileRepository.findAll().stream();
    }

    public void deleteFile(Long id) {
        fileRepository.deleteById(String.valueOf(id));
    }

}
