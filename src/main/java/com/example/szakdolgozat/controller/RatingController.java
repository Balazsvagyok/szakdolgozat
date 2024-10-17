package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.model.File;
import com.example.szakdolgozat.model.Rating;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.FileRepository;
import com.example.szakdolgozat.repository.PurchaseRepository;
import com.example.szakdolgozat.repository.RatingRepository;
import com.example.szakdolgozat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ratings")
public class RatingController {

    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PurchaseRepository purchaseRepository;

    @PostMapping("/rate")
    public String rateFile(@RequestParam Long fileId, @RequestParam int score,
                           @RequestParam(required = false) String comment,
                           RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = userRepository.findByUsername(authentication.getName());

        File file = fileRepository.findById(String.valueOf(fileId))
                .orElseThrow(() -> new RuntimeException("Fájl nem található"));

        boolean hasPurchased = purchaseRepository.existsByUserAndFile(loggedInUser, file);

        if (!hasPurchased) {
            redirectAttributes.addFlashAttribute("message", "Csak megvásárolt fájlokat értékelhetsz!");
            return "redirect:/files/" + fileId;
        }

        if (ratingRepository.findByUserAndFile(loggedInUser, file).isEmpty()) {
            Rating rating = new Rating();
            rating.setUser(loggedInUser);
            rating.setFile(file);
            rating.setScore(score);
            rating.setComment(comment);

            ratingRepository.save(rating);
            redirectAttributes.addFlashAttribute("message", "Értékelés sikeresen leadva!");
        } else {
            redirectAttributes.addFlashAttribute("message", "Már értékelted ezt a fájlt!");
        }

        return "redirect:/files/" + fileId;
    }
}

