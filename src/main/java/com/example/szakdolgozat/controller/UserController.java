package com.example.szakdolgozat.controller;

import com.example.szakdolgozat.model.Cart;
import com.example.szakdolgozat.model.User;
import com.example.szakdolgozat.repository.CartRepository;
import com.example.szakdolgozat.repository.UserRepository;
import com.example.szakdolgozat.service.CustomUserDetails;
import com.example.szakdolgozat.service.FileStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;


@Controller
@RequestMapping
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private CartRepository cartRepository;

    private String getLoggedInUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private User getLoggedInUser(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return userPrincipal.getUser();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    @GetMapping
    public String showHomePage(Model model) {
        String username = getLoggedInUsername();

        User user = userRepository.findByUsername(username);
        if (user != null) {
            String role = user.getRole();
            int id = user.getId();
            model.addAttribute("role", role);
            model.addAttribute("id", id);
        }
        return "index";
    }

    @GetMapping("/users")
    public String showAllUsers(Model model) {
        if (isAdmin()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User loggedInUser = getLoggedInUser(authentication);

            Iterable<User> userList = userRepository.findAll();
            model.addAttribute("users", userList);
            model.addAttribute("role", loggedInUser.getRole());
            model.addAttribute("id", loggedInUser.getId());
            return "users";
        } else {
            return "redirect: ";
        }
    }

    @GetMapping("/users/search")
    public String searchUser(@RequestParam("name") String name, Model model) {
        if (isAdmin()) {
            model.addAttribute("role", isAdmin() ? "ADMIN" : "USER");
            List<User> users = userRepository.findByUsernameContaining(name);
            if (!users.isEmpty()) {
                model.addAttribute("users", users);
            } else {
                model.addAttribute("message", "Nem található a megadott nevű felhasználó!");
            }
            return "users";
        } else {
            return "redirect: ";
        }
    }

    @GetMapping("/profile")
    public String showUserProfile(Model model) {
        String username = getLoggedInUsername();

        User user = userRepository.findByUsername(username);
        if (user != null) {
            String email = user.getEmail();
            String role = user.getRole();
            int id = user.getId();
            long purchases = fileStorageService.countPurchaseByUser(user);

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
    public String showRegistrationPage(Model model) {
        model.addAttribute("user", new User());
        return "registration";
    }

    @PostMapping("/save")
    public String registerNewProfile(@ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirectAttributes) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        try {
            User existingUser = userRepository.findByUsername(user.getUsername());
            if (existingUser != null) {
                result.rejectValue("username", "error", "A felhasználónév már létezik!");
                return "redirect:/registration";
            }

            String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            user.setRole("USER");
            user.setNewPassword("");
            userRepository.save(user);

            Cart cart = new Cart();
            cart.setUser(user);
            cartRepository.save(cart);

            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Hiba történt a mentés során.");
            return "redirect:/registration";
        }
    }


    @GetMapping("/edit/{id}")
    public String showUpdateForm(@PathVariable("id") int id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        model.addAttribute("id", loggedInUser.getId());
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("user", user);
        model.addAttribute("role", loggedInUser.getRole());
        return "update-user";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable("id") int id, Model model, User user, BindingResult result, RedirectAttributes redirectAttributes, HttpSession session) {
        if (result.hasErrors()) {
            user.setId(id);
            return "update-user";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);

        if (loggedInUser.getId().equals(id) || (loggedInUser.getRole().equals("ADMIN"))) {
            User existingUser = userRepository.findByUsername(user.getUsername());
            if (existingUser != null && existingUser.getId() != id) {
                model.addAttribute("role", loggedInUser.getRole());
                result.rejectValue("username", "error.user", "A felhasználónév már létezik!");
                return "update-user";
            } else {
                User userToUpdate = userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found!"));

                if (loggedInUser.getId().equals(id) && !loggedInUser.getRole().equals(user.getRole())) {
                    model.addAttribute("role", loggedInUser.getRole());
                    model.addAttribute("id", loggedInUser.getId());
                    result.rejectValue("role", "error.user", "A saját szereped nem változtatható!");
                    return "update-user";
                }

                userToUpdate.setUsername(user.getUsername());
                userToUpdate.setEmail(user.getEmail());
                userToUpdate.setRole(user.getRole());
                userRepository.save(userToUpdate);

                if (loggedInUser.getId().equals(id)) {
                    // SecurityContext frissítése az új adatokkal
                    CustomUserDetails updatedUserDetails = new CustomUserDetails(userToUpdate);
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(updatedUserDetails, authentication.getCredentials(), authentication.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // Session frissítés
                    session.setAttribute("loggedInUser", userToUpdate);
                }

                redirectAttributes.addFlashAttribute("message", "Felhasználó sikeresen frissítve!");
                return "redirect:/";
            }
        } else {
            redirectAttributes.addFlashAttribute("message", "Csak a saját felhasználódat módosíthatod!");
            return "redirect:/";
        }
    }


    @PostMapping("/update/password/{id}")
    public String updateUserPassword(@PathVariable("id") int id, User user, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            user.setId(id);
            return "update-user";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = getLoggedInUser(authentication);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));

        if (!loggedInUser.getId().equals(id) && !loggedInUser.getRole().equals("ADMIN")) {
            result.rejectValue("password", "error.user", "Csak a saját jelszavadat módosíthatod!");
            return "update-user";
        }

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        // Jelenlegi jelszó egyezés
        if (!bCryptPasswordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            result.rejectValue("password", "error.user", "A megadott jelszó nem egyezik a jelenlegi jelszóval!");
            return "update-user";
        }

        // Új jelszó min kritérium
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
    public String deleteUser(@PathVariable("id") int id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        userRepository.delete(user);
        return "redirect:/users";
    }

}