package com.example.auto4jobs.controllers;

import com.example.auto4jobs.dto.JobApplicationDTO;
import com.example.auto4jobs.dto.JobApplicationResponseDTO;
import com.example.auto4jobs.entities.JobApplication;
import com.example.auto4jobs.services.JobApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);

    @Autowired
    private JobApplicationService jobApplicationService;

    @PostMapping
    @PreAuthorize("hasRole('APPRENANT')")
    public ResponseEntity<?> applyForJob(@RequestBody JobApplicationDTO applicationDTO) {
        logger.info("Réception d'une demande de candidature pour l'offre ID: {}", applicationDTO.getJobOfferId());
        
        try {
            if (applicationDTO.getJobOfferId() == null) {
                logger.error("JobOfferId manquant dans la demande de candidature");
                return ResponseEntity.badRequest().body("JobOfferId is required");
            }
            
            logger.info("Traitement de la candidature via le service");
            JobApplicationResponseDTO response = jobApplicationService.applyForJob(applicationDTO);
            logger.info("Candidature créée avec succès, ID: {}", response.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation lors de la candidature: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("Conflit lors de la candidature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la candidature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur est survenue lors de la soumission de votre candidature: " + e.getMessage());
        }
    }

    @GetMapping("/candidate")
    @PreAuthorize("hasRole('APPRENANT')")
    public ResponseEntity<List<JobApplicationResponseDTO>> getCandidateApplications() {
        try {
            List<JobApplicationResponseDTO> applications = jobApplicationService.getCandidateApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            logger.error("Error fetching candidate applications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<List<JobApplicationResponseDTO>> getRecruiterApplications() {
        try {
            List<JobApplicationResponseDTO> applications = jobApplicationService.getRecruiterApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            logger.error("Error fetching recruiter applications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestBody Map<String, Object> statusUpdate) {
        try {
            String statusStr = (String) statusUpdate.get("status");
            String recruiterNotes = (String) statusUpdate.get("recruiterNotes");
            
            if (statusStr == null) {
                return ResponseEntity.badRequest().body("Status is required");
            }
            
            JobApplication.ApplicationStatus newStatus;
            try {
                newStatus = JobApplication.ApplicationStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid status value");
            }
            
            JobApplicationResponseDTO updatedApplication = 
                    jobApplicationService.updateApplicationStatus(applicationId, newStatus, recruiterNotes);
            return ResponseEntity.ok(updatedApplication);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating application status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating the application status");
        }
    }
} 