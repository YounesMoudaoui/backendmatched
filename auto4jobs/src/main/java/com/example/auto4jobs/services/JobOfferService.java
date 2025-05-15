package com.example.auto4jobs.services;

import com.example.auto4jobs.dto.JobOfferDTO;
import com.example.auto4jobs.dto.JobOfferResponseDTO;
import com.example.auto4jobs.entities.Entreprise;
import com.example.auto4jobs.entities.JobOffer;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.EntrepriseRepository;
import com.example.auto4jobs.repositories.JobOfferRepository;
import com.example.auto4jobs.repositories.UserRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobOfferService {

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    private User getAuthenticatedRecruiter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentRecruiterEmail = authentication.getName();
        User recruiter = userRepository.findByEmail(currentRecruiterEmail)
                .orElseThrow(() -> new IllegalStateException("Recruiter not found with email: " + currentRecruiterEmail));

        if (!"RECRUTEUR".equals(recruiter.getRole())) {
            throw new IllegalStateException("User is not authorized for this operation. Expected role RECRUTEUR.");
        }
        return recruiter;
    }

    @Transactional
    public JobOfferResponseDTO createJobOffer(JobOfferDTO jobOfferDTO) {
        User recruiter = getAuthenticatedRecruiter();
        Entreprise entreprise;
        Set<Entreprise> recruiterEntreprises = recruiter.getEntreprises();

        if (recruiter.isIntermediateRecruiter()) {
            if (jobOfferDTO.getEntrepriseId() == null) {
                throw new IllegalArgumentException("Entreprise ID is required for intermediate recruiters.");
            }
            entreprise = recruiterEntreprises.stream()
                    .filter(e -> e.getId().equals(jobOfferDTO.getEntrepriseId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Recruiter is not associated with the provided entreprise ID."));
        } else {
            if (recruiterEntreprises.isEmpty()) {
                throw new IllegalStateException("Recruiter is not associated with any entreprise.");
            }
            if (recruiterEntreprises.size() > 1) {
                throw new IllegalStateException("Non-intermediate recruiter is associated with multiple entreprises. Please contact admin.");
            }
            entreprise = recruiterEntreprises.iterator().next();
        }

        JobOffer jobOffer = new JobOffer();
        jobOffer.setTitrePoste(jobOfferDTO.getTitrePoste());
        jobOffer.setEntreprise(entreprise);
        jobOffer.setLocalisation(jobOfferDTO.getLocalisation());
        jobOffer.setDescriptionDetaillee(jobOfferDTO.getDescriptionDetaillee());
        jobOffer.setCompetencesTechniquesRequises(jobOfferDTO.getCompetencesTechniquesRequises());
        jobOffer.setCompetencesComportementalesRequises(jobOfferDTO.getCompetencesComportementalesRequises());
        jobOffer.setEducation(jobOfferDTO.getEducation());
        jobOffer.setTypeContrat(jobOfferDTO.getTypeContrat());
        jobOffer.setDureeContrat(jobOfferDTO.getDureeContrat());
        jobOffer.setTypeModalite(jobOfferDTO.getTypeModalite());
        jobOffer.setExperienceSouhaitee(jobOfferDTO.getExperienceSouhaitee());
        jobOffer.setCertificationsDemandees(jobOfferDTO.getCertificationsDemandees());
        jobOffer.setLangue(jobOfferDTO.getLangue());
        jobOffer.setRemuneration(jobOfferDTO.getRemuneration());
        jobOffer.setRecruiter(recruiter);

        // Consider adding createdAt/updatedAt logic here if not using @PrePersist/@PreUpdate in entity
        JobOffer savedJobOffer = jobOfferRepository.save(jobOffer);
        return convertToResponseDTO(savedJobOffer);
    }

    @Transactional(readOnly = true)
    public List<JobOfferResponseDTO> getJobOffersForRecruiter() {
        User recruiter = getAuthenticatedRecruiter();
        List<JobOffer> jobOffers = jobOfferRepository.findByRecruiterId(recruiter.getId());

        // Explicitly initialize Entreprise proxies to avoid LazyInitializationException
        for (JobOffer jobOffer : jobOffers) {
            if (jobOffer.getEntreprise() != null) {
                Hibernate.initialize(jobOffer.getEntreprise());
            }
        }

        return jobOffers.stream()
                        .map(this::convertToResponseDTO)
                        .collect(Collectors.toList());
    }

    private JobOfferResponseDTO convertToResponseDTO(JobOffer jobOffer) {
        if (jobOffer == null) return null;
        JobOfferResponseDTO dto = new JobOfferResponseDTO();
        dto.setId(jobOffer.getId());
        dto.setTitrePoste(jobOffer.getTitrePoste());
        
        Entreprise entreprise = jobOffer.getEntreprise();
        if (entreprise != null) {
            dto.setEntrepriseNom(entreprise.getNom());
            dto.setEntrepriseLogoUrl(entreprise.getLogoUrl());
        } else {
            dto.setEntrepriseNom("N/A");
            dto.setEntrepriseLogoUrl(null);
        }
        
        dto.setLocalisation(jobOffer.getLocalisation());
        if (jobOffer.getTypeContrat() != null) {
            dto.setTypeContrat(jobOffer.getTypeContrat().name());
        }
        if (jobOffer.getTypeModalite() != null) {
            dto.setTypeModalite(jobOffer.getTypeModalite().name());
        }
        dto.setActive(jobOffer.isActive());
        dto.setCreatedAt(jobOffer.getCreatedAt());
        dto.setUpdatedAt(jobOffer.getUpdatedAt());
        
        return dto;
    }

    @Transactional
    public JobOfferResponseDTO updateJobOffer(Long offerId, JobOfferDTO jobOfferDTO) {
        User recruiter = getAuthenticatedRecruiter();
        JobOffer jobOffer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Job offer not found with ID: " + offerId));

        if (!jobOffer.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalStateException("Recruiter is not authorized to update this job offer.");
        }

        // For now, we are not allowing the entreprise to be changed during an update.
        // If this needs to be supported, logic similar to createJobOffer for enterprise validation would be needed.

        jobOffer.setTitrePoste(jobOfferDTO.getTitrePoste());
        jobOffer.setLocalisation(jobOfferDTO.getLocalisation());
        jobOffer.setDescriptionDetaillee(jobOfferDTO.getDescriptionDetaillee());
        jobOffer.setCompetencesTechniquesRequises(jobOfferDTO.getCompetencesTechniquesRequises());
        jobOffer.setCompetencesComportementalesRequises(jobOfferDTO.getCompetencesComportementalesRequises());
        jobOffer.setEducation(jobOfferDTO.getEducation());
        jobOffer.setTypeContrat(jobOfferDTO.getTypeContrat());
        jobOffer.setDureeContrat(jobOfferDTO.getDureeContrat());
        jobOffer.setTypeModalite(jobOfferDTO.getTypeModalite());
        jobOffer.setExperienceSouhaitee(jobOfferDTO.getExperienceSouhaitee());
        jobOffer.setCertificationsDemandees(jobOfferDTO.getCertificationsDemandees());
        jobOffer.setLangue(jobOfferDTO.getLangue());
        jobOffer.setRemuneration(jobOfferDTO.getRemuneration());
        // recruiter and entreprise are not updated here by design in this version

        JobOffer updatedJobOffer = jobOfferRepository.save(jobOffer);
        return convertToResponseDTO(updatedJobOffer);
    }

    @Transactional
    public void deleteJobOffer(Long offerId) {
        User recruiter = getAuthenticatedRecruiter();
        JobOffer jobOffer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Job offer not found with ID: " + offerId));

        if (!jobOffer.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalStateException("Recruiter is not authorized to delete this job offer.");
        }

        jobOfferRepository.delete(jobOffer);
    }

    public List<JobOffer> getActiveJobOffers() {
        return jobOfferRepository.findAllByIsActiveTrue();
    }
} 