package com.example.auto4jobs.controllers;

import com.example.auto4jobs.entities.Centre;
import com.example.auto4jobs.entities.Entreprise;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_EXECUTIF')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return adminService.createUser(user);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
    }

    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return adminService.updateUser(id, user);
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
    public Entreprise createEntreprise(@RequestBody Entreprise entreprise) {
        return adminService.createEntreprise(entreprise);
    }

    @PutMapping("/entreprises/{id}")
    public Entreprise updateEntreprise(@PathVariable Long id, @RequestBody Entreprise entreprise) {
        return adminService.updateEntreprise(id, entreprise);
    }

    @DeleteMapping("/entreprises/{id}")
    public void deleteEntreprise(@PathVariable Long id) {
        adminService.deleteEntreprise(id);
    }

    @GetMapping("/entreprises")
    public List<Entreprise> getAllEntreprises() {
        return adminService.getAllEntreprises();
    }

    @PostMapping("/centres")
    public ResponseEntity<?> createCentre(@RequestBody Centre centre) {
        try {
            Centre savedCentre = adminService.createCentre(centre);
            return ResponseEntity.ok(savedCentre);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du centre: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur est survenue lors de la création du centre: " + e.getMessage());
        }
    }

    @PutMapping("/centres/{id}")
    public Centre updateCentre(@PathVariable Long id, @RequestBody Centre centre) {
        return adminService.updateCentre(id, centre);
    }

    @DeleteMapping("/centres/{id}")
    public void deleteCentre(@PathVariable Long id) {
        adminService.deleteCentre(id);
    }

    @GetMapping("/centres")
    public List<Centre> getAllCentres() {
        return adminService.getAllCentres();
    }

    @PostMapping("/assign-responsable")
    public User assignResponsableToCentre(@RequestParam Long userId, @RequestParam Long centreId) {
        return adminService.assignResponsableToCentre(userId, centreId);
    }

    @PostMapping("/assign-recruiter")
    public User assignRecruiterToEntreprises(@RequestParam Long userId, @RequestParam List<Long> entrepriseIds, @RequestParam boolean isIntermediate) {
        return adminService.assignRecruiterToEntreprises(userId, entrepriseIds, isIntermediate);
    }

    @PostMapping("/assign-intermediate-recruiter")
    public User assignIntermediateRecruiterToEntreprises(@RequestParam Long userId, @RequestParam String entrepriseNames) {
        return adminService.assignIntermediateRecruiterToEntreprises(userId, entrepriseNames);
    }

    @PutMapping("/recruiters/{userId}/entreprises")
    public User updateRecruiterEntreprises(@PathVariable Long userId, @RequestParam(required = false) List<Long> entrepriseIds, @RequestParam(required = false) String entrepriseNames, @RequestParam boolean isIntermediate) {
        return adminService.updateRecruiterEntreprises(userId, entrepriseIds != null ? entrepriseIds : List.of(), entrepriseNames != null ? entrepriseNames : "", isIntermediate);
    }

    @PostMapping("/upload-logo")
    public ResponseEntity<String> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            String uploadsDir = System.getProperty("user.dir") + "/uploads";
            Files.createDirectories(Paths.get(uploadsDir));
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String filename = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = Paths.get(uploadsDir, filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/" + filename;
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de l'upload du logo");
        }
    }
}