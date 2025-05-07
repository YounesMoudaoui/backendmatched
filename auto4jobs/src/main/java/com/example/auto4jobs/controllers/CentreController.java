package com.example.auto4jobs.controllers;

import com.example.auto4jobs.entities.Centre;
import com.example.auto4jobs.repositories.CentreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class CentreController {

    @Autowired
    private CentreRepository centreRepository;

    @GetMapping("/centres")
    public ResponseEntity<List<Centre>> getAllCentres() {
        return ResponseEntity.ok(centreRepository.findAll());
    }

    @PostMapping("/centres")
    public ResponseEntity<Centre> createCentre(@RequestBody Centre centre) {
        return ResponseEntity.ok(centreRepository.save(centre));
    }

    @PutMapping("/centres/{id}")
    public ResponseEntity<Centre> updateCentre(@PathVariable Long id, @RequestBody Centre centre) {
        if (!centreRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        centre.setId(id);
        return ResponseEntity.ok(centreRepository.save(centre));
    }

    @DeleteMapping("/centres/{id}")
    public ResponseEntity<Void> deleteCentre(@PathVariable Long id) {
        if (!centreRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        centreRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
} 