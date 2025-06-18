package com.example.auto4jobs.services;

import com.example.auto4jobs.dto.JobApplicationDTO;
import com.example.auto4jobs.dto.JobApplicationResponseDTO;
import com.example.auto4jobs.entities.JobApplication;
import com.example.auto4jobs.entities.JobOffer;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.JobApplicationRepository;
import com.example.auto4jobs.repositories.JobOfferRepository;
import com.example.auto4jobs.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobApplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationService.class);

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private UserRepository userRepository;

    public JobApplicationResponseDTO applyForJob(JobApplicationDTO applicationDTO) {
        logger.info("Début de la méthode applyForJob avec jobOfferId: {}", applicationDTO.getJobOfferId());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        logger.info("Email de l'utilisateur authentifié: {}", email);
        
        User candidate = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Utilisateur non trouvé pour l'email: {}", email);
                    return new IllegalStateException("User not authenticated");
                });
        logger.info("Candidat trouvé: id={}, nom={}", candidate.getId(), candidate.getLastName());

        JobOffer jobOffer = jobOfferRepository.findById(applicationDTO.getJobOfferId())
                .orElseThrow(() -> {
                    logger.error("Offre d'emploi non trouvée avec l'ID: {}", applicationDTO.getJobOfferId());
                    return new IllegalArgumentException("Job offer not found");
                });
        logger.info("Offre d'emploi trouvée: id={}, titre={}", jobOffer.getId(), jobOffer.getTitrePoste());

        // Check if user already applied for this job
        boolean alreadyApplied = jobApplicationRepository.existsByCandidateAndJobOffer(candidate, jobOffer);
        logger.info("Le candidat a-t-il déjà postulé? {}", alreadyApplied);
        
        if (alreadyApplied) {
            logger.warn("Le candidat a déjà postulé à cette offre. Candidat ID: {}, Offre ID: {}", 
                    candidate.getId(), jobOffer.getId());
            throw new IllegalStateException("You have already applied for this job");
        }

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobOffer(jobOffer);
        application.setApplicationDate(LocalDateTime.now());
        application.setStatus(JobApplication.ApplicationStatus.PENDING);
        application.setMessage(applicationDTO.getMessage());
        
        logger.info("Sauvegarde de la candidature en base de données");
        JobApplication savedApplication = jobApplicationRepository.save(application);
        logger.info("Candidature sauvegardée avec succès. ID: {}", savedApplication.getId());
        
        return convertToResponseDTO(savedApplication);
    }

    public List<JobApplicationResponseDTO> getCandidateApplications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User candidate = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        List<JobApplication> applications = jobApplicationRepository.findByCandidate(candidate);
        return applications.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<JobApplicationResponseDTO> getRecruiterApplications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        List<JobApplication> applications = jobApplicationRepository.findByJobOffer_RecruiterId(recruiter.getId());
        return applications.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public JobApplicationResponseDTO updateApplicationStatus(Long applicationId, JobApplication.ApplicationStatus newStatus, String recruiterNotes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        // Verify the recruiter is authorized to update this application
        if (!application.getJobOffer().getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalStateException("You are not authorized to update this application");
        }

        application.setStatus(newStatus);
        if (recruiterNotes != null) {
            application.setRecruiterNotes(recruiterNotes);
        }

        JobApplication updatedApplication = jobApplicationRepository.save(application);
        return convertToResponseDTO(updatedApplication);
    }

    private JobApplicationResponseDTO convertToResponseDTO(JobApplication application) {
        JobApplicationResponseDTO dto = new JobApplicationResponseDTO();
        dto.setId(application.getId());
        dto.setCandidateId(application.getCandidate().getId());
        dto.setCandidateName(application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName());
        dto.setJobOfferId(application.getJobOffer().getId());
        dto.setJobTitle(application.getJobOffer().getTitrePoste());
        dto.setCompanyName(application.getJobOffer().getEntreprise().getNom());
        dto.setApplicationDate(application.getApplicationDate());
        dto.setStatus(application.getStatus());
        dto.setMessage(application.getMessage());
        dto.setRecruiterNotes(application.getRecruiterNotes());
        return dto;
    }
} 