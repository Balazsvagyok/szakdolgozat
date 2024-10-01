package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.message.ResponseFile;
import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.Purchase;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.FileRepository;
import com.example.szakdolgozat.repository.PurchaseRepository;
import com.example.szakdolgozat.repository.UserRepository;
import com.example.szakdolgozat.service.CustomUserDetails;
import com.example.szakdolgozat.service.FileStorageService;
import com.example.szakdolgozat.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FileController {
    @Autowired
    private FileStorageService storageService;
    @Autowired
    private VideoService videoService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private PurchaseRepository purchaseRepository;

    private static final String UPLOAD_DIR = "uploads/";


    public void listVideos(Model model, RedirectAttributes redirectAttributes) {
        try {
            Iterable<String> videos = videoService.listAllVideos();
            model.addAttribute("videos", videos);
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Error reading video files");
        }
    }

    @PostMapping("/upload/video")
    public String uploadVideo(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully: " + file.getOriginalFilename());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Failed to upload file: " + e.getMessage());
        }
        return "redirect:/files";
    }


    // Megvásárolható fájlok listázása az alábbi úton
    @GetMapping("/files")
    public String getListFiles(Model model, RedirectAttributes redirectAttributes) {
        // Bejelentkezett felhasználó lekérése és szerep meghatározása, Collection -> String
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream().map(Object::toString).collect(Collectors.joining(","));

        List<ResponseFile> files = storageService.getAllFiles().map(file -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/files/")
                    .path(String.valueOf(file.getId()))
                    .toUriString();

            return new ResponseFile(
                    file.getId(),
                    file.getName(),
                    fileDownloadUri,
                    file.getType(),
                    file.getData().length,
                    file.getDescription(),
                    file.getPrice(),
                    file.getUploader());
        }).collect(Collectors.toList());

        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        User loggedInUser = userPrincipal.getUser();

        listVideos(model, redirectAttributes);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("files", files);
        model.addAttribute("role", role);
        return "files";
    }

    // Megvásárolt fájlok listázása az alábbi úton
    @GetMapping("/purchases")
    public String getListPurchases(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream().map(Object::toString).collect(Collectors.joining(","));

        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        User loggedInUser = userPrincipal.getUser();

        List<ResponseFile> files = storageService.getPurchasedFiles(loggedInUser).stream().map(file -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/files/")
                    .path(String.valueOf(file.getId()))
                    .toUriString();

            return new ResponseFile(
                    file.getId(),
                    file.getName(),
                    fileDownloadUri,
                    file.getType(),
                    file.getData().length,
                    file.getDescription(),
                    file.getPrice(),
                    file.getUploader());
        }).collect(Collectors.toList());

        listVideos(model, redirectAttributes);
        model.addAttribute("user", loggedInUser);
        model.addAttribute("files", files);
        model.addAttribute("role", role);
        return "purchases";
    }

    // Fájlok vásárlása és adatbázisba mentése
    @PostMapping("/purchase/{fileId}")
    public String purchaseFile(@PathVariable Integer fileId, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        User loggedInUser = userPrincipal.getUser();

        File file = fileRepository.findById(String.valueOf(fileId))
                .orElseThrow(() -> new RuntimeException("File not found!"));

        Purchase purchase = new Purchase();
        purchase.setUser(loggedInUser);
        purchase.setFile(file);
        purchase.setPurchaseDate(new Date(System.currentTimeMillis()));

        purchaseRepository.save(purchase);

        redirectAttributes.addFlashAttribute("message", "Fájl sikeresen megvásárolva!");
        return "redirect:/files";
    }



    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("description") String description,
                             @RequestParam("price") Double price, RedirectAttributes redirectAttributes) {
        String message;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername());

            File newFile = new File();
            newFile.setName(file.getOriginalFilename());
            newFile.setType(file.getContentType());
            newFile.setDescription(description);
            newFile.setPrice(price);
            newFile.setData(file.getBytes());
            newFile.setUploader(user);

            storageService.saveFile(newFile);
            message = "Sikeres fájl feltöltés: " + file.getOriginalFilename();
        } catch (Exception e) {
            message = "Fájl feltöltése sikertelen: " + file.getOriginalFilename() + "!";
        }
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/files";
    }


    @GetMapping("/files/edit/{id}")
    public String editFileForm(@PathVariable Long id, Model model) {
        File file = storageService.getFile(String.valueOf(id));
        model.addAttribute("file", file);
        return "update-file";
    }

    @PostMapping("/files/update/{id}")
    public String updateFile(@PathVariable("id") Long id, @ModelAttribute File file,
                             RedirectAttributes redirectAttributes) {
        String message;
        try {
            File existingFile = storageService.getFile(String.valueOf(id));
            existingFile.setDescription(file.getDescription());
            existingFile.setPrice(file.getPrice());
            storageService.saveFile(existingFile);
            message = "Sikeres mentés: " + existingFile.getName();
        } catch (Exception e) {
            message = "Fájl mentése sikertelen: " + file.getName() + "!";
        }
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/files";
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        File file = storageService.getFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(file.getData());
    }


    @PostMapping("/files/{id}")
    public String deleteFile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            storageService.deleteFile(id);
            redirectAttributes.addFlashAttribute("message", "Fájl sikeresen törölve lett!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Fájl törlése sikertelen volt!");
        }
        return "redirect:/files";
    }

}
