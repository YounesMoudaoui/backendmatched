package com.example.auto4jobs.controllers;

import com.example.auto4jobs.services.CVService;
import com.example.auto4jobs.services.OllamaMatchingService;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    private static final Logger logger = LoggerFactory.getLogger(MatchingController.class);

    @Autowired
    private OllamaMatchingService matchingService;
    
    @Autowired
    private CVService cvService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Extrait les compétences du CV de l'utilisateur courant
     * 
     * @return Les compétences extraites du CV
     */
    @GetMapping("/extract-cv-skills")
    @PreAuthorize("hasAnyRole('APPRENANT', 'LAUREAT')")
    public ResponseEntity<?> extractCurrentUserCVSkills() {
        try {
            // Vérifier si l'utilisateur a un CV
            if (!cvService.currentUserHasCV()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", "Aucun CV trouvé pour cet utilisateur"));
            }
            
            // Récupérer l'ID de l'utilisateur authentifié
            Long userId = getCurrentUserId();
            
            Map<String, Object> skills = matchingService.extractCVSkills(userId);
            return ResponseEntity.ok(skills);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de l'extraction des compétences du CV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Erreur d'état lors de l'extraction des compétences du CV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'extraction des compétences du CV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Une erreur est survenue lors de l'extraction des compétences du CV"));
        }
    }

    /**
     * Trouve les offres d'emploi qui correspondent le mieux au CV de l'utilisateur courant
     * 
     * @return Liste des offres d'emploi avec leurs scores de correspondance
     */
    @GetMapping("/job-matches")
    @PreAuthorize("hasAnyRole('APPRENANT', 'LAUREAT')")
    public ResponseEntity<?> getJobMatchesForCurrentUser() {
        try {
            // Vérifier si l'utilisateur a un CV
            if (!cvService.currentUserHasCV()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", "Aucun CV trouvé pour cet utilisateur"));
            }
            
            // Récupérer l'ID de l'utilisateur authentifié
            Long userId = getCurrentUserId();
            
            List<Map<String, Object>> matches = matchingService.matchJobOffersForUser(userId);
            
            // Si aucune offre n'est trouvée, retourner une liste vide avec un message
            if (matches.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("matches", Collections.emptyList());
                response.put("message", "Aucune offre d'emploi active trouvée pour le matching");
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de la recherche de correspondances: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Erreur d'état lors de la recherche de correspondances: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la recherche de correspondances", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Une erreur est survenue lors de la recherche de correspondances"));
        }
    }

    /**
     * Force le recalcul des correspondances pour l'utilisateur courant
     * 
     * @return Liste des offres d'emploi avec leurs scores de correspondance recalculés
     */
    @PostMapping("/refresh-job-matches")
    @PreAuthorize("hasAnyRole('APPRENANT', 'LAUREAT')")
    public ResponseEntity<?> refreshJobMatchesForCurrentUser() {
        try {
            // Vérifier si l'utilisateur a un CV
            if (!cvService.currentUserHasCV()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", "Aucun CV trouvé pour cet utilisateur"));
            }
            
            // Récupérer l'ID de l'utilisateur authentifié
            Long userId = getCurrentUserId();
            
            // Forcer le recalcul des correspondances
            List<Map<String, Object>> matches = matchingService.forceMatchJobOffersForUser(userId);
            
            // Si aucune offre n'est trouvée, retourner une liste vide avec un message
            if (matches.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("matches", Collections.emptyList());
                response.put("message", "Aucune offre d'emploi active trouvée pour le matching");
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors du recalcul des correspondances: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Erreur d'état lors du recalcul des correspondances: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors du recalcul des correspondances", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Une erreur est survenue lors du recalcul des correspondances"));
        }
    }

    /**
     * Trouve les offres d'emploi qui correspondent le mieux au CV d'un utilisateur spécifique
     * (endpoint réservé aux administrateurs)
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des offres d'emploi avec leurs scores de correspondance
     */
    @GetMapping("/job-matches/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getJobMatchesForUser(@PathVariable Long userId) {
        try {
            // Vérifier si l'utilisateur a un CV
            if (!cvService.userHasCV(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", "Aucun CV trouvé pour cet utilisateur"));
            }
            
            List<Map<String, Object>> matches = matchingService.matchJobOffersForUser(userId);
            
            // Si aucune offre n'est trouvée, retourner une liste vide avec un message
            if (matches.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("matches", Collections.emptyList());
                response.put("message", "Aucune offre d'emploi active trouvée pour le matching");
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de la recherche de correspondances: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Erreur d'état lors de la recherche de correspondances: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la recherche de correspondances", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Une erreur est survenue lors de la recherche de correspondances"));
        }
    }

    /**
     * Récupère l'ID de l'utilisateur authentifié
     * 
     * @return ID de l'utilisateur authentifié
     * @throws IllegalStateException si l'utilisateur n'est pas authentifié ou si son ID n'est pas un nombre
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        
        // Récupérer l'email de l'utilisateur authentifié
        String email = authentication.getName();
        
        // Utiliser le repository pour trouver l'utilisateur par email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé avec l'email: " + email));
        
        return user.getId();
    }
} 