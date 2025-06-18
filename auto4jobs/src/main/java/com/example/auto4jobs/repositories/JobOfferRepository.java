package com.example.auto4jobs.repositories;

import com.example.auto4jobs.entities.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    @Query("SELECT j FROM JobOffer j WHERE j.recruiter.id = :recruiterId")
    List<JobOffer> findByRecruiterId(@Param("recruiterId") Long recruiterId);
    List<JobOffer> findByEntrepriseId(Long entrepriseId);
    List<JobOffer> findAllByIsActiveTrue();
    List<JobOffer> findByIsActiveTrue();
} 