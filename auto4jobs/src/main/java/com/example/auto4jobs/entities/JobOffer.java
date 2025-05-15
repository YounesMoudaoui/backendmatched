package com.example.auto4jobs.entities;

import com.example.auto4jobs.entities.enums.ContractType;
import com.example.auto4jobs.entities.enums.OfferModality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "job_offers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre_poste", nullable = false)
    private String titrePoste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    // Logo de l'entreprise is available via entreprise.getLogoUrl()

    @Column(name = "localisation", nullable = false)
    private String localisation;

    @Lob
    @Column(name = "description_detaillee", nullable = false, columnDefinition = "TEXT")
    private String descriptionDetaillee;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_offer_competences_techniques", joinColumns = @JoinColumn(name = "job_offer_id"))
    @Column(name = "competence_technique")
    private Set<String> competencesTechniquesRequises;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_offer_competences_comportementales", joinColumns = @JoinColumn(name = "job_offer_id"))
    @Column(name = "competence_comportementale")
    private Set<String> competencesComportementalesRequises;

    @Column(name = "education")
    private String education;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false)
    private ContractType typeContrat;

    @Column(name = "duree_contrat") // Optionnelle
    private String dureeContrat;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_modalite") // Optionnelle
    private OfferModality typeModalite;

    @Column(name = "experience_souhaitee")
    private String experienceSouhaitee;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_offer_certifications", joinColumns = @JoinColumn(name = "job_offer_id"))
    @Column(name = "certification")
    private Set<String> certificationsDemandees;

    @Column(name = "langue")
    private String langue;

    @Column(name = "remuneration") // Optionnelle
    private String remuneration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Default to active
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.util.Date();
        updatedAt = new java.util.Date(); // Also set updatedAt on creation
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.util.Date();
    }
} 