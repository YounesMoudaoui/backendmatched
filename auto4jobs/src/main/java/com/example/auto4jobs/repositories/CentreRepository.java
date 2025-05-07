package com.example.auto4jobs.repositories;

import com.example.auto4jobs.entities.Centre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CentreRepository extends JpaRepository<Centre, Long> {
    Optional<Centre> findByEmailDomain(String emailDomain);
}