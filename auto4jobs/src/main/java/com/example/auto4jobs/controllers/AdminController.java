package com.example.auto4jobs.controllers;

import com.example.auto4jobs.entities.Centre;
import org.springframework.web.bind.annotation.RequestMethod;
import com.example.auto4jobs.entities.Entreprise;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.services.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_EXECUTIF')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = adminService.createUser(user);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) { // Example for unique constraint violation like email
            logger.warn("Failed to create user due to data integrity issue: {}", user.getEmail(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User creation failed: " + e.getRootCause().getMessage());
        } catch (Exception e) {
            logger.error("Error creating user: {}", user, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while creating the user.");
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            // To provide a 404, AdminService.deleteUser would need to check existence first.
            // For now, we assume success if no exception is thrown.
            adminService.deleteUser(id);
            return ResponseEntity.ok().body("User with id " + id + " processed for deletion.");
        } catch (Exception e) {
            logger.error("Error deleting user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while deleting the user.");
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updatedUser = adminService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) { // Assuming AdminService throws this if user not found
            logger.warn("Failed to update non-existent user with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
             logger.warn("Failed to update user due to data integrity issue for id {}: {}", id, e.getRootCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User update failed: " + e.getRootCause().getMessage());
        } catch (Exception e) {
            logger.error("Error updating user with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while updating the user.");
        }
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/recruiters")
    public List<User> getAllRecruiters() {
        return adminService.getAllRecruiters();
    }

    @PostMapping("/entreprises")
    public ResponseEntity<?> createEntreprise(@RequestBody Entreprise entreprise) {
        try {
            Entreprise createdEntreprise = adminService.createEntreprise(entreprise);
            return new ResponseEntity<>(createdEntreprise, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) { 
            logger.warn("Failed to create entreprise due to data integrity issue: {}", entreprise.getNom(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Entreprise creation failed: " + e.getRootCause().getMessage());
        } catch (Exception e) {
            logger.error("Error creating entreprise: {}", entreprise, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while creating the entreprise.");
        }
    }

    @PutMapping("/entreprises/{id}")
    public ResponseEntity<?> updateEntreprise(@PathVariable Long id, @RequestBody Entreprise entreprise) {
        try {
            Entreprise updatedEntreprise = adminService.updateEntreprise(id, entreprise);
            return ResponseEntity.ok(updatedEntreprise);
        } catch (IllegalArgumentException e) { // Assuming AdminService throws this if entreprise not found
            logger.warn("Failed to update non-existent entreprise with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
             logger.warn("Failed to update entreprise due to data integrity issue for id {}: {}", id, e.getRootCause().getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Entreprise update failed: " + e.getRootCause().getMessage());
        } catch (Exception e) {
            logger.error("Error updating entreprise with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while updating the entreprise.");
        }
    }

    @DeleteMapping("/entreprises/{id}")
    public ResponseEntity<?> deleteEntreprise(@PathVariable Long id) {
        try {
            // To provide a 404, AdminService.deleteEntreprise would need to check existence first.
            adminService.deleteEntreprise(id);
            return ResponseEntity.ok().body("Entreprise with id " + id + " processed for deletion.");
        } catch (Exception e) {
            logger.error("Error deleting entreprise with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while deleting the entreprise.");
        }
    }

    @GetMapping("/entreprises")
    public List<Entreprise> getAllEntreprises() {
        return adminService.getAllEntreprises();
    }

    @PostMapping("/centres")
    public ResponseEntity<?> createCentre(@RequestBody Centre centre) {
        try {
            Centre savedCentre = adminService.createCentre(centre);
            return new ResponseEntity<>(savedCentre, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create centre: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating centre: {}", centre, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while creating the centre: " + e.getMessage());
        }
    }

    @PutMapping("/centres/{id}")
    public ResponseEntity<?> updateCentre(@PathVariable Long id, @RequestBody Centre centre) {
        try {
            Centre updatedCentre = adminService.updateCentre(id, centre);
            return ResponseEntity.ok(updatedCentre);
        } catch (IllegalArgumentException e) { // Assuming AdminService throws this if centre not found or invalid data
            logger.warn("Failed to update centre with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating centre with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while updating the centre.");
        }
    }

    @DeleteMapping("/centres/{id}")
    public ResponseEntity<?> deleteCentre(@PathVariable Long id) {
        try {
            // To provide a 404, AdminService.deleteCentre would need to check existence first.
            adminService.deleteCentre(id);
            return ResponseEntity.ok().body("Centre with id " + id + " processed for deletion.");
        } catch (Exception e) {
            logger.error("Error deleting centre with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while deleting the centre.");
        }
    }

    @GetMapping("/centres")
    public List<Centre> getAllCentres() {
        return adminService.getAllCentres();
    }

    @PostMapping("/assign-responsable")
    public ResponseEntity<?> assignResponsableToCentre(@RequestParam Long userId, @RequestParam Long centreId) {
        try {
            User updatedUser = adminService.assignResponsableToCentre(userId, centreId);
            return ResponseEntity.ok(updatedUser);
        } catch (NoSuchElementException e) { // Or a custom 'NotFoundException' from service
            logger.warn("Failed to assign responsable: User or Centre not found. UserId: {}, CentreId: {}. Error: {}", userId, centreId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) { // For other business logic errors from service
            logger.warn("Failed to assign responsable. UserId: {}, CentreId: {}. Error: {}", userId, centreId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error assigning responsable to centre. UserId: {}, CentreId: {}", userId, centreId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during assignment.");
        }
    }

    @PostMapping("/assign-recruiter")
    public ResponseEntity<?> assignRecruiterToEntreprises(@RequestParam Long userId, @RequestParam List<Long> entrepriseIds, @RequestParam boolean isIntermediate) {
        try {
            User updatedUser = adminService.assignRecruiterToEntreprises(userId, entrepriseIds, isIntermediate);
            return ResponseEntity.ok(updatedUser);
        } catch (NoSuchElementException e) {
            logger.warn("Failed to assign recruiter: User or Entreprise not found. UserId: {}, EntrepriseIds: {}. Error: {}", userId, entrepriseIds, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Failed to assign recruiter. UserId: {}, EntrepriseIds: {}. Error: {}", userId, entrepriseIds, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error assigning recruiter to entreprises. UserId: {}, EntrepriseIds: {}", userId, entrepriseIds, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during assignment.");
        }
    }

    @PostMapping("/assign-intermediate-recruiter")
    public ResponseEntity<?> assignIntermediateRecruiterToEntreprises(@RequestParam Long userId, @RequestParam String entrepriseNames) {
        try {
            User updatedUser = adminService.assignIntermediateRecruiterToEntreprises(userId, entrepriseNames);
            return ResponseEntity.ok(updatedUser);
        } catch (NoSuchElementException e) {
            logger.warn("Failed to assign intermediate recruiter: User not found. UserId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Failed to assign intermediate recruiter. UserId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error assigning intermediate recruiter. UserId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during assignment.");
        }
    }

    @PutMapping("/recruiters/{userId}/entreprises")
    public ResponseEntity<?> updateRecruiterEntreprises(@PathVariable Long userId, @RequestParam(required = false) List<Long> entrepriseIds, @RequestParam(required = false) String entrepriseNames, @RequestParam boolean isIntermediate) {
        try {
            User updatedUser = adminService.updateRecruiterEntreprises(userId, entrepriseIds != null ? entrepriseIds : List.of(), entrepriseNames != null ? entrepriseNames : "", isIntermediate);
            return ResponseEntity.ok(updatedUser);
        } catch (NoSuchElementException e) {
            logger.warn("Failed to update recruiter entreprises: User or Entreprise not found. UserId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Failed to update recruiter entreprises. UserId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating recruiter entreprises. UserId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during update.");
        }
    }

    @PostMapping("/upload-logo")
    public ResponseEntity<String> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            String uploadsDir = System.getProperty("user.dir") + "/uploads";
            Files.createDirectories(Paths.get(uploadsDir));
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Original filename is missing.");
            }
            String cleanedFilename = StringUtils.cleanPath(originalFilename);
            // Sanitize the filename further to prevent directory traversal
            if (cleanedFilename.contains("..")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid filename.");
            }
            String filename = System.currentTimeMillis() + "_" + cleanedFilename;
            Path filePath = Paths.get(uploadsDir, filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/" + filename;
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            // Log the exception for debugging purposes
            System.err.println("Error during logo upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de l'upload du logo: " + e.getMessage());
        }
    }
}