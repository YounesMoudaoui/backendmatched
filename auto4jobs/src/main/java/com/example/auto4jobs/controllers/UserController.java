package com.example.auto4jobs.controllers;

import com.example.auto4jobs.dto.UserProfileDTO;
import com.example.auto4jobs.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()") // Ensures the user is authenticated
    public ResponseEntity<UserProfileDTO> getCurrentUserProfile() {
        try {
            UserProfileDTO userProfile = userService.getCurrentUserProfile();
            return ResponseEntity.ok(userProfile);
        } catch (IllegalStateException e) {
            // This might happen if the authenticated user somehow isn't in the DB
            // Or if SecurityContextHolder has issues. Log appropriately.
            // For now, returning 404, but 500 might also be suitable depending on cause.
            return ResponseEntity.status(404).body(null); 
        }
    }
} 