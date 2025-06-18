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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobOfferService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobOfferService.class);

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

    @Transactional(readOnly = true)
    public List<JobOffer> getActiveJobOffers() {
        try {
            logger.debug("Fetching all active job offers");
            List<JobOffer> offers = jobOfferRepository.findAllByIsActiveTrue();
            
            if (offers == null) {
                logger.warn("Repository returned null for active job offers");
                return new ArrayList<>();
            }
            
            // Résoudre le problème de chargement paresseux (lazy loading)
            for (JobOffer offer : offers) {
                // Initialiser explicitement les relations pour éviter les LazyInitializationException
                if (offer.getEntreprise() != null) {
                    Hibernate.initialize(offer.getEntreprise());
                }
                
                // Convertir les collections en listes pour éviter les problèmes de sérialisation
                if (offer.getCompetencesTechniquesRequises() != null) {
                    Hibernate.initialize(offer.getCompetencesTechniquesRequises());
                }
                
                if (offer.getCompetencesComportementalesRequises() != null) {
                    Hibernate.initialize(offer.getCompetencesComportementalesRequises());
                }
                
                if (offer.getCertificationsDemandees() != null) {
                    Hibernate.initialize(offer.getCertificationsDemandees());
                }
            }
            
            logger.debug("Found {} active job offers", offers.size());
            return offers;
        } catch (Exception e) {
            logger.error("Error fetching active job offers", e);
            throw e;
        }
    }
    
    // Méthode pour convertir les offres en format compatible avec le frontend
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveJobOffersAsDTO() {
        List<JobOffer> offers = getActiveJobOffers();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (JobOffer offer : offers) {
            Map<String, Object> offerMap = new HashMap<>();
            offerMap.put("id", offer.getId());
            offerMap.put("titrePoste", offer.getTitrePoste());
            
            // Gestion de l'entreprise
            Map<String, Object> entrepriseMap = new HashMap<>();
            if (offer.getEntreprise() != null) {
                entrepriseMap.put("id", offer.getEntreprise().getId());
                entrepriseMap.put("nom", offer.getEntreprise().getNom());
                entrepriseMap.put("logoUrl", offer.getEntreprise().getLogoUrl());
            }
            offerMap.put("entreprise", entrepriseMap);
            
            offerMap.put("localisation", offer.getLocalisation());
            offerMap.put("descriptionDetaillee", offer.getDescriptionDetaillee());
            offerMap.put("education", offer.getEducation());
            offerMap.put("typeContrat", offer.getTypeContrat() != null ? offer.getTypeContrat().name() : null);
            offerMap.put("dureeContrat", offer.getDureeContrat());
            offerMap.put("typeModalite", offer.getTypeModalite() != null ? offer.getTypeModalite().name() : null);
            offerMap.put("experienceSouhaitee", offer.getExperienceSouhaitee());
            offerMap.put("langue", offer.getLangue());
            offerMap.put("remuneration", offer.getRemuneration());
            offerMap.put("isActive", offer.isActive());
            offerMap.put("createdAt", offer.getCreatedAt());
            offerMap.put("updatedAt", offer.getUpdatedAt());
            
            // Conversion des collections en listes
            offerMap.put("competencesTechniquesRequises", offer.getCompetencesTechniquesRequises() != null ? 
                    new ArrayList<>(offer.getCompetencesTechniquesRequises()) : new ArrayList<>());
            
            offerMap.put("competencesComportementalesRequises", offer.getCompetencesComportementalesRequises() != null ? 
                    new ArrayList<>(offer.getCompetencesComportementalesRequises()) : new ArrayList<>());
            
            offerMap.put("certificationsDemandees", offer.getCertificationsDemandees() != null ? 
                    new ArrayList<>(offer.getCertificationsDemandees()) : new ArrayList<>());
            
            result.add(offerMap);
        }
        
        return result;
    }

    @Transactional(readOnly = true)
    public JobOfferDTO getJobOfferForEdit(Long offerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalStateException("User not found with email: " + currentUserEmail));

        JobOffer jobOffer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Job offer not found with ID: " + offerId));

        // Si l'utilisateur est un recruteur, vérifier qu'il est bien le propriétaire de l'offre
        if ("RECRUTEUR".equals(currentUser.getRole()) && !jobOffer.getRecruiter().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Recruiter is not authorized to edit this job offer.");
        }

        // Si l'utilisateur est un apprenant, vérifier que l'offre est active
        if ("APPRENANT".equals(currentUser.getRole()) && !jobOffer.isActive()) {
            throw new IllegalStateException("This job offer is not active.");
        }

        // Convertir l'entité JobOffer en JobOfferDTO pour l'édition
        JobOfferDTO dto = new JobOfferDTO();
        dto.setTitrePoste(jobOffer.getTitrePoste());
        dto.setEntrepriseId(jobOffer.getEntreprise().getId());
        dto.setLocalisation(jobOffer.getLocalisation());
        dto.setDescriptionDetaillee(jobOffer.getDescriptionDetaillee());
        dto.setCompetencesTechniquesRequises(jobOffer.getCompetencesTechniquesRequises());
        dto.setCompetencesComportementalesRequises(jobOffer.getCompetencesComportementalesRequises());
        dto.setEducation(jobOffer.getEducation());
        dto.setTypeContrat(jobOffer.getTypeContrat());
        dto.setDureeContrat(jobOffer.getDureeContrat());
        dto.setTypeModalite(jobOffer.getTypeModalite());
        dto.setExperienceSouhaitee(jobOffer.getExperienceSouhaitee());
        dto.setCertificationsDemandees(jobOffer.getCertificationsDemandees());
        dto.setLangue(jobOffer.getLangue());
        dto.setRemuneration(jobOffer.getRemuneration());

        return dto;
    }
} 