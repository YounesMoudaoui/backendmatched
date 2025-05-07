package com.example.auto4jobs.controllers;

import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.services.CandidateValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/validation")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_EXECUTIF')")
public class CandidateValidationController {

    @Autowired
    private CandidateValidationService validationService;

    @PostMapping("/validate/{userId}")
    public User validateCandidate(@PathVariable Long userId) {
        return validationService.validateCandidate(userId);
    }

    @GetMapping("/pending-candidates")
    public List<User> getPendingCandidates() {
        return validationService.getPendingCandidates();
    }

    @GetMapping("/pending-laureats")
    public List<User> getPendingLaureats() {
        return validationService.getPendingLaureats();
    }
}