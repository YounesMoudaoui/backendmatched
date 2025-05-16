package com.example.auto4jobs.controllers;

import com.example.auto4jobs.dto.UserProfileDTO;
import com.example.auto4jobs.services.CVService;
import com.example.auto4jobs.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CVService cvService;

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

    @PostMapping("/me/cv")
    @PreAuthorize("hasAnyRole('APPRENANT', 'LAUREAT')")
    public ResponseEntity<?> uploadCV(@RequestParam("cv") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Veuillez sélectionner un fichier à télécharger");
        }

        try {
            Map<String, Object> fileInfo = cvService.saveCV(file);
            return ResponseEntity.ok(fileInfo);
        } catch (IllegalArgumentException e) {
            logger.error("Format de fichier invalide: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde du fichier: {}", e.getMessage());
            return ResponseEntity.status(500).body("Échec du téléchargement du fichier: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inattendue lors du téléchargement du CV: {}", e.getMessage());
            return ResponseEntity.status(500).body("Une erreur inattendue s'est produite");
        }
    }
} 