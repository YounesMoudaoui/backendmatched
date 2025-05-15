package com.example.auto4jobs.repositories;

import com.example.auto4jobs.entities.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    List<JobOffer> findByRecruiterId(Long recruiterId);
    List<JobOffer> findByEntrepriseId(Long entrepriseId);
    List<JobOffer> findAllByIsActiveTrue();
} 