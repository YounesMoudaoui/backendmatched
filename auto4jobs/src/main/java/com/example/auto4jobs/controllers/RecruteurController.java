package com.example.auto4jobs.controllers;

import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class RecruteurController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/recruteurs")
    public ResponseEntity<List<User>> getAllRecruteurs() {
        return ResponseEntity.ok(userRepository.findByRole("RECRUTEUR"));
    }
} 