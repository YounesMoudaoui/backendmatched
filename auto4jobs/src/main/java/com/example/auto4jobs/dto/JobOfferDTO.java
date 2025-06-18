package com.example.auto4jobs.dto;

import com.example.auto4jobs.entities.JobOffer.ContractType;
import com.example.auto4jobs.entities.JobOffer.OfferModality;
import lombok.Data;

import java.util.Set;

@Data
public class JobOfferDTO {
    private String titrePoste;
    private Long entrepriseId; // To specify which company if recruiter has multiple
    private String localisation;
    private String descriptionDetaillee;
    private Set<String> competencesTechniquesRequises;
    private Set<String> competencesComportementalesRequises;
    private String education;
    private ContractType typeContrat;
    private String dureeContrat;        // Optionnelle
    private OfferModality typeModalite;   // Optionnelle
    private String experienceSouhaitee;
    private Set<String> certificationsDemandees;
    private String langue;
    private String remuneration;        // Optionnelle
} 