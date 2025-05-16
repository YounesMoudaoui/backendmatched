package com.example.auto4jobs.dto;

import java.util.Set;

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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isIntermediateRecruiter() {
        return isIntermediateRecruiter;
    }

    public void setIntermediateRecruiter(boolean intermediateRecruiter) {
        isIntermediateRecruiter = intermediateRecruiter;
    }

    public Set<EntrepriseSlimDTO> getEntreprises() {
        return entreprises;
    }

    public void setEntreprises(Set<EntrepriseSlimDTO> entreprises) {
        this.entreprises = entreprises;
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    public Long getCvUploadDate() {
        return cvUploadDate;
    }

    public void setCvUploadDate(Long cvUploadDate) {
        this.cvUploadDate = cvUploadDate;
    }
} 