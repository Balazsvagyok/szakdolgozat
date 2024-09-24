package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.message.ResponseFile;
import com.example.szakdolgozat.message.ResponseMessage;
import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.service.CustomUserDetails;
import com.example.szakdolgozat.service.FileStorageService;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FileController {
    @Autowired
    private FileStorageService storageService;


    @GetMapping("/files")
    public String getListFiles(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userPrincipal = (CustomUserDetails) auth.getPrincipal();
        User loggedInUser = userPrincipal.getUser();

        if (loggedInUser.getRole().equals("ADMIN") || loggedInUser.getRole().equals("USER")) {
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
                        file.getPrice());
            }).collect(Collectors.toList());

            model.addAttribute("files", files);
            model.addAttribute("role", loggedInUser.getRole());
            return "files";
        } else {
            return "redirect:/";
        }
    }


    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("description") String description,
                             @RequestParam("price") Double price, RedirectAttributes redirectAttributes) {
        String message = "";
        try {
            File newFile = new File();
            newFile.setName(file.getOriginalFilename());
            newFile.setType(file.getContentType());
            newFile.setDescription(description);
            newFile.setPrice(price);
            newFile.setData(file.getBytes()); // Assuming you have a data field to store the file content

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
    public String updateFile(@PathVariable("id") Long id, @ModelAttribute File file, BindingResult result, RedirectAttributes redirectAttributes) {
        String message = "";
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
