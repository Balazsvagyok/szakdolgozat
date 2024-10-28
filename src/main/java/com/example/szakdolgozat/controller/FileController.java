package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.message.ResponseFile;
import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.Purchase;
import com.example.szakdolgozat.model.Rating;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.FileRepository;
import com.example.szakdolgozat.repository.PurchaseRepository;
import com.example.szakdolgozat.repository.RatingRepository;
import com.example.szakdolgozat.service.CustomUserDetails;
import com.example.szakdolgozat.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
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
    @Autowired
    private RatingRepository ratingRepository;

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

    // Megvásárolható fájlok listázása
    @GetMapping("/files")
    public String getListFiles(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);
        String role = getUserRole(authentication);

        List<ResponseFile> files = getResponseFiles();

        model.addAttribute("user", loggedInUser);
        model.addAttribute("files", files);
        model.addAttribute("role", role);
        model.addAttribute("id", loggedInUser.getId());
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
        model.addAttribute("id", loggedInUser.getId());
        return "purchases";
    }

    // Fájlok vásárlása és adatbázisba mentése
    @PostMapping("/purchase/{fileId}")
    public String purchaseFile(@PathVariable Integer fileId, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);

        File file = fileRepository.findById(String.valueOf(fileId))
                .orElseThrow(() -> new RuntimeException("Fájl nem található!"));

        boolean alreadyPurchased = purchaseRepository.findByUserAndFile(loggedInUser, file).isPresent();

        if (alreadyPurchased) {
            redirectAttributes.addFlashAttribute("message", "Ezt a fájlt már megvásároltad!");
            return "redirect:/files";
        }

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

            if (file.getSize() > 16 * 1024 * 1024) {
                message = "Fájl feltöltése sikertelen: A fájl mérete max. 16 MB!";
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


    @PostMapping("/files/edit/{id}")
    public String editFileForm(@PathVariable Long id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);

        File file = storageService.getFile(String.valueOf(id));
        model.addAttribute("file", file);
        model.addAttribute("role", loggedInUser.getRole());
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

    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId, HttpServletResponse response) throws IOException {

        File dbFile = fileRepository.findById(String.valueOf(fileId))
                .orElseThrow(() -> new RuntimeException("Fájl nem található!"));

        // Fájl MIME típusának meghatározása
        String mimeType = URLConnection.guessContentTypeFromName(dbFile.getName());

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        response.setContentType(mimeType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + dbFile.getName() + "\"");
        response.setContentLength(dbFile.getData().length);

        // Fájl adatainak írása a kimenetre
        try (OutputStream os = response.getOutputStream()) {
            os.write(dbFile.getData(), 0, dbFile.getData().length);
        }
    }




    @GetMapping("/files/{id}")
    public String getFile(@PathVariable String id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);

        File file = storageService.getFile(id);
        List<Rating> ratings = ratingRepository.findByFile(file);
        model.addAttribute("file", file);
        model.addAttribute("ratings", ratings);
        model.addAttribute("role", loggedInUser.getRole());
        model.addAttribute("id", loggedInUser.getId());

        return "open-file";
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
