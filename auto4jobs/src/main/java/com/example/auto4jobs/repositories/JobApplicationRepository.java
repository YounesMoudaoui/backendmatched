package com.example.auto4jobs.repositories;

import com.example.auto4jobs.entities.JobApplication;
import com.example.auto4jobs.entities.JobOffer;
import com.example.auto4jobs.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    
    List<JobApplication> findByCandidate(User candidate);
    
    List<JobApplication> findByJobOffer(JobOffer jobOffer);
    
    Optional<JobApplication> findByCandidateAndJobOffer(User candidate, JobOffer jobOffer);
    
    List<JobApplication> findByJobOffer_Entreprise_Id(Long entrepriseId);
    
    List<JobApplication> findByJobOffer_RecruiterId(Long recruiterId);
    
    boolean existsByCandidateAndJobOffer(User candidate, JobOffer jobOffer);
} 