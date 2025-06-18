package com.example.auto4jobs.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone")
    private String phone;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "is_validated", nullable = false)
    private boolean isValidated = false;

    @Column(name = "is_intermediate_recruiter", nullable = false)
    private boolean intermediateRecruiter = false;

    @ManyToOne
    @JoinColumn(name = "centre_id")
    private Centre centre;

    @ManyToMany
    @JoinTable(
            name = "user_entreprises",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "entreprise_id")
    )
    private Set<Entreprise> entreprises;
    
    @Column(name = "cv_path")
    private String cvPath;
    
    @Column(name = "cv_upload_date")
    private Long cvUploadDate;
    
    @Lob
    @Column(name = "cv_data", columnDefinition = "LONGBLOB")
    private byte[] cvData;
    
    @Column(name = "cv_filename")
    private String cvFilename;
    
    @Column(name = "cv_content_type")
    private String cvContentType;

    public void setIsValidated(boolean isValidated) {
        this.isValidated = isValidated;
    }

    public void setIsIntermediateRecruiter(boolean isIntermediateRecruiter) {
        this.intermediateRecruiter = isIntermediateRecruiter;
    }

    public String getCvContentType() {
        return cvContentType;
    }

    public void setCvContentType(String cvContentType) {
        this.cvContentType = cvContentType;
    }
    
    public String getCvFilename() {
        return cvFilename;
    }
    
    public void setCvFilename(String cvFilename) {
        this.cvFilename = cvFilename;
    }
}