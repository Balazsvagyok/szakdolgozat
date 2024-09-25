//package com.example.szakdolgozat.controller;
//
//import com.example.szakdolgozat.service.VideoService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.io.IOException;
//import java.nio.file.*;
//import java.util.List;
//
//@Controller
//@RequestMapping("/videos")
//public class VideoController {
//    @Autowired
//    private VideoService videoService;
//    private static final String UPLOAD_DIR = "uploads/";
//
//    @GetMapping("/list")
//    public String listVideos(Model model, RedirectAttributes redirectAttributes) {
//        try {
//            Iterable<String> videos = videoService.listAllVideos();
//            model.addAttribute("videos", videos);
//            return "files";
//        } catch (IOException e) {
//            e.printStackTrace();
//            redirectAttributes.addFlashAttribute("message", "Error reading video files");
//            return "redirect:/files";
//        }
//    }
//
//    @PostMapping("/upload")
//    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
//        try {
//            Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
//            Files.createDirectories(path.getParent());
//            Files.write(path, file.getBytes());
//            return ResponseEntity.ok("File uploaded successfully: " + file.getOriginalFilename());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Failed to upload file: " + e.getMessage());
//        }
//    }
//}
