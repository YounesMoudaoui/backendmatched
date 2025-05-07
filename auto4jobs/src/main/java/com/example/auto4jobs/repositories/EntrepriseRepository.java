package com.example.auto4jobs.repositories;

import com.example.auto4jobs.entities.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    Optional<Entreprise> findByNom(String nom);
}