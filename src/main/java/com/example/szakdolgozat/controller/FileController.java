package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.message.ResponseFile;
import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.Purchase;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.FileRepository;
import com.example.szakdolgozat.repository.PurchaseRepository;
import com.example.szakdolgozat.service.CustomUserDetails;
import com.example.szakdolgozat.service.FileStorageService;
import com.example.szakdolgozat.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FileController {
    @Autowired
    private FileStorageService storageService;

    // @Autowired
    // private VideoService videoService;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private PurchaseRepository purchaseRepository;

    // private static final String UPLOAD_DIR = "uploads/";

    private User getLoggedInUser(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return userPrincipal.getUser();
    }

    // Collection to String
    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private List<ResponseFile> getResponseFiles() {
        return storageService.getAllFiles().map(file -> {
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
                    file.getUploader(),
                    file.isDeleted());
        }).collect(Collectors.toList());
    }

    private List<ResponseFile> getPurchasedFiles(User user) {
        return storageService.getPurchasedFiles(user).stream().map(file -> {
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
                    file.getUploader(),
                    file.isDeleted());
        }).collect(Collectors.toList());
    }

//    public void listVideos(Model model, RedirectAttributes redirectAttributes) {
//        try {
//            Iterable<String> videos = videoService.listAllVideos();
//            model.addAttribute("videos", videos);
//        } catch (IOException e) {
//            e.printStackTrace();
//            redirectAttributes.addFlashAttribute("message", "Hiba a videófájlok beolvasása közben!");
//        }
//    }

//    @PostMapping("/upload/video")
//    public String uploadVideo(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
//        try {
//            Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
//            Files.createDirectories(path.getParent());
//            Files.write(path, file.getBytes());
//            redirectAttributes.addFlashAttribute("message", "Fájl feltöltése sikeres: " + file.getOriginalFilename());
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("message", "Fájl feltöltése sikertelen: " + e.getMessage());
//        }
//        return "redirect:/files";
//    }


    // Megvásárolható fájlok listázása
    @GetMapping("/files")
    public String getListFiles(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);
        String role = getUserRole(authentication);

        List<ResponseFile> files = getResponseFiles();
        // listVideos(model, redirectAttributes);

        model.addAttribute("user", loggedInUser);
        model.addAttribute("files", files);
        model.addAttribute("role", role);
        return "files";
    }

    // Megvásárolt fájlok listázása
    @GetMapping("/purchases")
    public String getListPurchases(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);
        String role = getUserRole(authentication);

        List<ResponseFile> files = getPurchasedFiles(loggedInUser);
        // listVideos(model, redirectAttributes);

        model.addAttribute("user", loggedInUser);
        model.addAttribute("files", files);
        model.addAttribute("role", role);
        return "purchases";
    }

    // Fájlok vásárlása és adatbázisba mentése
    @PostMapping("/purchase/{fileId}")
    public String purchaseFile(@PathVariable Integer fileId, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);

        File file = fileRepository.findById(String.valueOf(fileId))
                .orElseThrow(() -> new RuntimeException("Fájl nem található!"));

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
            if (isFileExists(file.getOriginalFilename())) {
                message = "Fájl feltöltése sikertelen: Már létezik fájl ugyan azzal a névvel!";
                redirectAttributes.addFlashAttribute("message", message);
                return "redirect:/files";
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User loggedInUser = getLoggedInUser(authentication);

            File newFile = createNewFile(file, description, price, loggedInUser);
            storageService.saveFile(newFile);
            message = "Sikeres fájl feltöltés: " + file.getOriginalFilename();

        } catch (Exception e) {
            message = "Fájl feltöltése sikertelen: " + file.getOriginalFilename() + "! Hiba: " + e.getMessage();
        }
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/files";
    }

    private boolean isFileExists(String fileName) {
        return fileRepository.findByNameAndDeletedFalse(fileName).isPresent();
    }

    private File createNewFile(MultipartFile file, String description, Double price, User uploader) throws IOException {
        File newFile = new File();
        newFile.setName(file.getOriginalFilename());
        newFile.setType(file.getContentType());
        newFile.setDescription(description);
        newFile.setPrice(price);
        newFile.setData(file.getBytes());
        newFile.setUploader(uploader);
        return newFile;
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


    // Ha valaki megvásárolta, akkor nem lehet törölni (javítani)
    @PostMapping("/files/{id}")
    public String deleteFile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            File file = fileRepository.findById(String.valueOf(id))
                    .orElseThrow(() -> new RuntimeException("Fájl nem található"));
            file.setDeleted(true);
            fileRepository.save(file);

            redirectAttributes.addFlashAttribute("message", "Fájl sikeresen törölve lett!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Fájl törlése sikertelen volt!");
        }
        return "redirect:/files";
    }

}
