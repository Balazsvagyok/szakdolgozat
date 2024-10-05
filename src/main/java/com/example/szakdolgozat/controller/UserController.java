package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.PurchaseRepository;
import com.example.szakdolgozat.repository.UserRepository;
import com.example.szakdolgozat.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @GetMapping
    public String homePage(Model model) {
        Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
        String name = loggedInUser.getName();

        User user = userRepository.findByUsername(name);
        if (user != null) {
            String role = user.getRole();
            int id = user.getId();
//          System.out.println(role);
            model.addAttribute("role", role);
            model.addAttribute("id", id);
        }
        return "index";
    }

//    @GetMapping("")
//    public String viewHomePage() {
////        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
////        String name = auth.getName();
////
////        User user = userRepository.findByUsername(name);
////        String role = user.getRole();
////        System.out.println(role);
//
//        return "index";
//    }

//    @GetMapping(path = "/all")
//    public @ResponseBody Iterable<User> getAllUsers() {
//        return userRepository.findAll();
//    }

    @GetMapping("/users")
    public String showAllUsers(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            Iterable<User> userList = userRepository.findAll();
            model.addAttribute("users", userList);
            return "users";
        } else {
            return "redirect: ";
        }
    }

    @GetMapping("/profile")
    public String userProfile(Model model) {
        Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
        String name = loggedInUser.getName();

        User user = userRepository.findByUsername(name);
        if (user != null) {
            String username = user.getUsername();
            String email = user.getEmail();
            String role = user.getRole();
            int id = user.getId();
            long purchases = purchaseRepository.countByUser(user);

            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("role", role);
            model.addAttribute("id", id);
            model.addAttribute("purchases", purchases);

            return "profile";
        }

        return "redirect: ";
    }

    @GetMapping("/registration")
    public String add(Model model) {
        model.addAttribute("user", new User());
        return "registration";
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public String saveUser(@ModelAttribute("user") User user) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        try {
            String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            user.setRole("USER");
            userRepository.save(user);
            return "redirect:/login";
        } catch (Exception ex) {
            return "redirect:/registration?error=username_already_exists";
        }
    }

    @PostMapping("/edit/{id}")
    public String showUpdateForm(@PathVariable("id") int id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        model.addAttribute("user", user);
        return "update-user";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable("id") int id, User user, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            user.setId(id);
            return "update-user";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        User loggedInUser = userPrincipal.getUser();

        if (loggedInUser.getId().equals(id) || loggedInUser.getRole().equals("ADMIN")) {

            User existingUser = userRepository.findByUsername(user.getUsername());
            if (existingUser.getUsername().equals(user.getUsername()) && existingUser.getId() != id){
                redirectAttributes.addFlashAttribute("message", "A felhasználónév már létezik!");
                return "redirect:/";
            }

            User userToUpdate = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found!"));
            userToUpdate.setUsername(user.getUsername());
            userToUpdate.setEmail(user.getEmail());
            userRepository.save(userToUpdate);

            redirectAttributes.addFlashAttribute("message", "Felhasználó sikeresen frissítve!");
        } else {
            redirectAttributes.addFlashAttribute("message", "Csak a saját felhasználódat módosíthatod!");
        }
        return "redirect:/";
    }


    @PostMapping("/update/password/{id}")
    public String updateUserPassword(@PathVariable("id") int id, User user, BindingResult result, RedirectAttributes redirectAttributes){
        if (result.hasErrors()) {
            user.setId(id);
            return "update-user";
        }

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        if (!bCryptPasswordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            result.rejectValue("password", "error.user", "A megadott jelszó nem egyezik a jelenlegi jelszóval!");
            return "update-user";
        }

        if (user.getNewPassword().length() < 4) {
            result.rejectValue("newPassword", "error.user", "A megadott új jelszó hosszabb kell legyen, mint 3 karakter!");
            return "update-user";
        }

        existingUser.setPassword(bCryptPasswordEncoder.encode(user.getNewPassword()));
        userRepository.save(existingUser);
        redirectAttributes.addFlashAttribute("message", "A jelszó sikeresen frissítve lett!");
        return "redirect:/";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") int id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        userRepository.delete(user);
        return "redirect:/users";
    }

}