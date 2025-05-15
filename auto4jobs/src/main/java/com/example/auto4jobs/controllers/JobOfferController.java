package com.example.auto4jobs.controllers;

import com.example.auto4jobs.dto.JobOfferDTO;
import com.example.auto4jobs.dto.JobOfferResponseDTO;
import com.example.auto4jobs.entities.JobOffer;
import com.example.auto4jobs.services.JobOfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-offers")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Adjust origins as needed
public class JobOfferController {

    private static final Logger logger = LoggerFactory.getLogger(JobOfferController.class);

    @Autowired
    private JobOfferService jobOfferService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<?> createJobOffer(@RequestBody JobOfferDTO jobOfferDTO) {
        try {
            JobOfferResponseDTO createdJobOfferDTO = jobOfferService.createJobOffer(jobOfferDTO);
            return new ResponseEntity<>(createdJobOfferDTO, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating job offer for DTO: {}", jobOfferDTO, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating the job offer.");
        }
    }

    @GetMapping("/my-offers")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<List<JobOfferResponseDTO>> getMyJobOffers() {
        try {
            List<JobOfferResponseDTO> jobOffers = jobOfferService.getJobOffersForRecruiter();
            return ResponseEntity.ok(jobOffers);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (Exception e) {
            logger.error("Error fetching job offers for authenticated recruiter", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{offerId}")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<?> updateJobOffer(@PathVariable Long offerId, @RequestBody JobOfferDTO jobOfferDTO) {
        try {
            JobOfferResponseDTO updatedJobOfferDTO = jobOfferService.updateJobOffer(offerId, jobOfferDTO);
            return ResponseEntity.ok(updatedJobOfferDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating job offer with ID: {}", offerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating the job offer.");
        }
    }

    @DeleteMapping("/{offerId}")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<?> deleteJobOffer(@PathVariable Long offerId) {
        try {
            jobOfferService.deleteJobOffer(offerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting job offer with ID: {}", offerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the job offer.");
        }
    }

    @GetMapping
    public ResponseEntity<List<JobOffer>> getAllActiveJobOffers() {
        try {
            List<JobOffer> jobOffers = jobOfferService.getActiveJobOffers();
            return ResponseEntity.ok(jobOffers);
        } catch (Exception e) {
            logger.error("Error fetching all active job offers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
} 