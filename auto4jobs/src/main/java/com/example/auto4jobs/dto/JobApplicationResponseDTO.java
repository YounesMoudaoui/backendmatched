package com.example.auto4jobs.dto;

import com.example.auto4jobs.entities.JobApplication.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobApplicationResponseDTO {
    private Long id;
    private Long candidateId;
    private String candidateName;
    private Long jobOfferId;
    private String jobTitle;
    private String companyName;
    private LocalDateTime applicationDate;
    private ApplicationStatus status;
    private String message;
    private String recruiterNotes;
} 