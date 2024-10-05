package com.example.szakdolgozat.service;

import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.Purchase;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.FileRepository;
import com.example.szakdolgozat.repository.PurchaseRepository;
import com.example.szakdolgozat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PurchaseRepository purchaseRepository;

//    public File store(MultipartFile file) throws IOException {
//        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
//        File File = new File(fileName, file.getContentType(), file.getBytes());
//
//        return fileRepository.save(File);
//    }

    public List<File> getPurchasedFiles(User user) {
        return purchaseRepository.findByUser(user).stream()
                .map(Purchase::getFile)
                .collect(Collectors.toList());
    }

    public void saveFile(File file) throws IOException {
        fileRepository.save(file);
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

    public long countPurchaseByUser(User user){
        return countPurchaseByUser(user);
    }
}