package com.example.auto4jobs.controllers;

import com.example.auto4jobs.entities.Entreprise;
import com.example.auto4jobs.repositories.EntrepriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class EntrepriseController {

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @GetMapping("/entreprises")
    public ResponseEntity<List<Entreprise>> getAllEntreprises() {
        return ResponseEntity.ok(entrepriseRepository.findAll());
    }

    @PostMapping("/entreprises")
    public ResponseEntity<Entreprise> createEntreprise(@RequestBody Entreprise entreprise) {
        return ResponseEntity.ok(entrepriseRepository.save(entreprise));
    }

    @PutMapping("/entreprises/{id}")
    public ResponseEntity<Entreprise> updateEntreprise(@PathVariable Long id, @RequestBody Entreprise entreprise) {
        if (!entrepriseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        entreprise.setId(id);
        return ResponseEntity.ok(entrepriseRepository.save(entreprise));
    }

    @DeleteMapping("/entreprises/{id}")
    public ResponseEntity<Void> deleteEntreprise(@PathVariable Long id) {
        if (!entrepriseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        entrepriseRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
} 