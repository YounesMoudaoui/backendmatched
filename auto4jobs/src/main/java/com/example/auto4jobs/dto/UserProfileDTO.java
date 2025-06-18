package com.example.auto4jobs.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserProfileDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean isIntermediateRecruiter;
    private Set<EntrepriseSlimDTO> entreprises;
    private String cvPath;
    private Long cvUploadDate;
    private boolean hasCvInDatabase;
    private String cvContentType;

    public UserProfileDTO(Long id, String email, String firstName, String lastName, String role, boolean isIntermediateRecruiter, Set<EntrepriseSlimDTO> entreprises) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isIntermediateRecruiter = isIntermediateRecruiter;
        this.entreprises = entreprises;
    }

    public UserProfileDTO(Long id, String email, String firstName, String lastName, String role, boolean isIntermediateRecruiter, Set<EntrepriseSlimDTO> entreprises, String cvPath, Long cvUploadDate) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isIntermediateRecruiter = isIntermediateRecruiter;
        this.entreprises = entreprises;
        this.cvPath = cvPath;
        this.cvUploadDate = cvUploadDate;
    }
    
    public UserProfileDTO(Long id, String email, String firstName, String lastName, String role, boolean isIntermediateRecruiter, Set<EntrepriseSlimDTO> entreprises, String cvPath, Long cvUploadDate, boolean hasCvInDatabase, String cvContentType) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isIntermediateRecruiter = isIntermediateRecruiter;
        this.entreprises = entreprises;
        this.cvPath = cvPath;
        this.cvUploadDate = cvUploadDate;
        this.hasCvInDatabase = hasCvInDatabase;
        this.cvContentType = cvContentType;
    }
} 