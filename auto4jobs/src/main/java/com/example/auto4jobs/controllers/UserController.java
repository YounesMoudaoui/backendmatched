package com.example.auto4jobs.controllers;

import com.example.auto4jobs.dto.UserProfileDTO;
import com.example.auto4jobs.dto.UserProfileUpdateDTO;
import org.springframework.web.bind.annotation.RequestMethod;
import com.example.auto4jobs.services.CVService;
import com.example.auto4jobs.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
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

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCurrentUserProfile(@RequestBody UserProfileUpdateDTO profileUpdateDTO) {
        try {
            UserProfileDTO updatedProfile = userService.updateCurrentUserProfile(profileUpdateDTO);
            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du profil utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur est survenue lors de la mise à jour du profil");
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
            logger.info("CV téléchargé avec succès: {}", fileInfo.get("fileName"));
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
    
    @GetMapping("/me/cv/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> viewCV(@PathVariable String filename) {
        try {
            logger.info("Demande de visualisation du CV: {}", filename);
            // Construire le chemin du fichier
            Path filePath = Paths.get(cvService.getUploadDir()).resolve(filename);
            logger.info("Chemin complet du fichier: {}", filePath.toAbsolutePath());
            
            Resource resource = new UrlResource(filePath.toUri());
            
            // Vérifier que le fichier existe et est lisible
            if (resource.exists() && resource.isReadable()) {
                // Déterminer le type de contenu
                String contentType = determineContentType(filename);
                logger.info("Fichier trouvé, envoi avec le type de contenu: {}", contentType);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                logger.warn("Fichier non trouvé ou non lisible: {}", filePath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fichier non trouvé");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du CV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la récupération du fichier: " + e.getMessage());
        }
    }
    
    @GetMapping("/me/cv-from-db")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> viewCVFromDatabase() {
        try {
            logger.info("Demande de visualisation du CV depuis la base de données");
            
            // Récupérer les données du CV depuis la base de données
            byte[] cvData = cvService.getCurrentUserCVDataFromDatabase();
            
            // Vérifier que les données existent
            if (cvData != null && cvData.length > 0) {
                // Récupérer le type de contenu
                String contentType = cvService.getCurrentUserCVContentType();
                if (contentType == null || contentType.isEmpty()) {
                    contentType = "application/pdf"; // Par défaut, on suppose que c'est un PDF
                }
                
                logger.info("CV trouvé dans la base de données, envoi avec le type de contenu: {}", contentType);
                
                // Créer une ressource à partir des données binaires
                ByteArrayResource resource = new ByteArrayResource(cvData);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv.pdf\"")
                    .body(resource);
            } else {
                logger.warn("Aucun CV trouvé dans la base de données");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun CV trouvé dans la base de données");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du CV depuis la base de données: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la récupération du CV: " + e.getMessage());
        }
    }
    
    @GetMapping("/me/cv-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserCVInfo() {
        try {
            Map<String, Object> cvInfo = userService.getCurrentUserCVInfo();
            return ResponseEntity.ok(cvInfo);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des informations du CV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la récupération des informations du CV: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/me/cv")
    @PreAuthorize("hasAnyRole('APPRENANT', 'LAUREAT')")
    public ResponseEntity<?> deleteCV() {
        try {
            boolean deleted = cvService.deleteCurrentUserCV();
            if (deleted) {
                return ResponseEntity.ok().body(Map.of("success", true, "message", "CV supprimé avec succès"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun CV trouvé pour cet utilisateur");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du CV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la suppression du CV: " + e.getMessage());
        }
    }
    
    private String determineContentType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.toLowerCase().endsWith(".doc")) {
            return "application/msword";
        } else if (filename.toLowerCase().endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else {
            return "application/octet-stream";
        }
    }
} 