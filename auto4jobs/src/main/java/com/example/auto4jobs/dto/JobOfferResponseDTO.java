package com.example.auto4jobs.dto;

import com.example.auto4jobs.entities.enums.ContractType;
import com.example.auto4jobs.entities.enums.OfferModality;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferResponseDTO {
    private Long id;
    private String titrePoste;
    private String entrepriseNom; 
    private String entrepriseLogoUrl;
    private String localisation;
    private String typeContrat; // Store as String representation of ContractType enum
    private String typeModalite; // Store as String representation of OfferModality enum
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;
    // Add any other fields you might need for the list view
} 