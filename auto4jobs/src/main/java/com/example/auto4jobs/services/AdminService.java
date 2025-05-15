package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.Centre;
import com.example.auto4jobs.entities.Entreprise;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.CentreRepository;
import com.example.auto4jobs.repositories.EntrepriseRepository;
import com.example.auto4jobs.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private CentreRepository centreRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        logger.info("Initializing system admin...");
        // Initialize Admin User
        User admin = userRepository.findByEmail("admin@auto4jobs.com")
            .orElseGet(() -> {
                logger.info("Admin user not found, creating a new one.");
                User newUser = new User();
                newUser.setEmail("admin@auto4jobs.com");
                newUser.setFirstName("Admin");
                newUser.setLastName("System");
                newUser.setRole("ADMIN");
                return newUser;
            });
        if (admin.getId() != null) { // Check if it's an existing user being updated
            logger.info("Admin user found, ensuring password and validation status are up to date.");
        }
        admin.setPassword(passwordEncoder.encode("admin123")); // Force password for debug, consider changing for prod
        admin.setIsValidated(true);
        userRepository.save(admin);
        logger.info("Admin user initialization complete.");
    }

    @Transactional
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole().equals("APPRENANT") || user.getRole().equals("LAUREAT")) {
            user.setIsValidated(false); // Candidates require validation
        } else {
            user.setIsValidated(true); // Other roles are validated by default
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        logger.info("Attempting to delete user with ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            logger.warn("User with ID: {} not found for deletion.", userId);
            // Consider throwing a custom NotFoundException here if preferred
            return; 
        }
        userRepository.deleteById(userId);
        logger.info("User with ID: {} deleted successfully.", userId);
    }

    @Transactional
    public Entreprise createEntreprise(Entreprise entreprise) {
        return entrepriseRepository.save(entreprise);
    }

    @Transactional
    public Centre createCentre(Centre centre) {
        try {
            logger.debug("Attempting to create centre: {}", centre);
            if (centre.getVille() == null || centre.getVille().trim().isEmpty()) {
                throw new IllegalArgumentException("La ville est obligatoire");
            }
            if (centre.getNom() == null || centre.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom est obligatoire");
            }
            
            Centre savedCentre = centreRepository.save(centre);
            logger.info("Centre created successfully: {}", savedCentre);
            return savedCentre;
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create centre due to invalid argument: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error during centre creation: {}", centre, e);
            throw e;
        }
    }

    @Transactional
    public User assignResponsableToCentre(Long userId, Long centreId) {
        User user = userRepository.findById(userId).orElseThrow();
        Centre centre = centreRepository.findById(centreId).orElseThrow();
        user.setCentre(centre);
        user.setRole("RESPONSABLE_CENTRE");
        return userRepository.save(user);
    }

    @Transactional
    public User assignRecruiterToEntreprises(Long userId, List<Long> entrepriseIds, boolean isIntermediate) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.getRole().equals("RECRUTEUR")) {
            throw new IllegalStateException("User must be a recruiter");
        }
        user.setIsIntermediateRecruiter(isIntermediate);
        Set<Entreprise> entreprises = user.getEntreprises();
        entreprises.clear(); // Clear existing enterprises

        if (!isIntermediate && entrepriseIds.size() > 1) {
            throw new IllegalStateException("Non-intermediate recruiter can only be associated with one enterprise");
        }

        for (Long entrepriseId : entrepriseIds) {
            Entreprise entreprise = entrepriseRepository.findById(entrepriseId).orElseThrow();
            entreprises.add(entreprise);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User assignIntermediateRecruiterToEntreprises(Long userId, String entrepriseNames) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.getRole().equals("RECRUTEUR")) {
            throw new IllegalStateException("User must be a recruiter");
        }
        user.setIsIntermediateRecruiter(true);
        Set<Entreprise> entreprises = user.getEntreprises();
        entreprises.clear(); // Clear existing enterprises

        List<String> names = Arrays.asList(entrepriseNames.split(",")).stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toList());

        for (String name : names) {
            Entreprise entreprise = entrepriseRepository.findByNom(name)
                    .orElseGet(() -> {
                        Entreprise newEntreprise = new Entreprise();
                        newEntreprise.setNom(name);
                        return entrepriseRepository.save(newEntreprise);
                    });
            entreprises.add(entreprise);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateRecruiterEntreprises(Long userId, List<Long> entrepriseIds, String entrepriseNames, boolean isIntermediate) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.getRole().equals("RECRUTEUR")) {
            throw new IllegalStateException("User must be a recruiter");
        }
        user.setIsIntermediateRecruiter(isIntermediate);
        Set<Entreprise> entreprises = user.getEntreprises();
        entreprises.clear(); // Clear existing enterprises

        if (isIntermediate) {
            List<String> names = Arrays.asList(entrepriseNames.split(",")).stream()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());
            for (String name : names) {
                Entreprise entreprise = entrepriseRepository.findByNom(name)
                        .orElseGet(() -> {
                            Entreprise newEntreprise = new Entreprise();
                            newEntreprise.setNom(name);
                            return entrepriseRepository.save(newEntreprise);
                        });
                entreprises.add(entreprise);
            }
        } else {
            if (entrepriseIds.size() > 1) {
                throw new IllegalStateException("Non-intermediate recruiter can only be associated with one enterprise");
            }
            for (Long entrepriseId : entrepriseIds) {
                Entreprise entreprise = entrepriseRepository.findById(entrepriseId).orElseThrow();
                entreprises.add(entreprise);
            }
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Entreprise> getAllEntreprises() {
        return entrepriseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Centre> getAllCentres() {
        return centreRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getAllRecruiters() {
        return userRepository.findByRole("RECRUTEUR");
    }

    @Transactional
    public User updateUser(Long userId, User updatedUser) {
        logger.info("Attempting to update user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User with ID: {} not found for update.", userId);
                    // Ideally, throw a specific "NotFoundException" here
                    return new RuntimeException("User not found with id: " + userId); 
                });
        
        logger.debug("Updating user fields for ID: {}", userId);
        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setEmail(updatedUser.getEmail());
        user.setPhone(updatedUser.getPhone());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            logger.debug("Updating password for user ID: {}", userId);
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        User savedUser = userRepository.save(user);
        logger.info("User with ID: {} updated successfully.", userId);
        return savedUser;
    }

    @Transactional
    public Entreprise updateEntreprise(Long entrepriseId, Entreprise updatedEntreprise) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId).orElseThrow();
        entreprise.setNom(updatedEntreprise.getNom());
        entreprise.setLogoUrl(updatedEntreprise.getLogoUrl());
        entreprise.setCareerPageUrl(updatedEntreprise.getCareerPageUrl());
        return entrepriseRepository.save(entreprise);
    }

    @Transactional
    public Centre updateCentre(Long centreId, Centre updatedCentre) {
        Centre centre = centreRepository.findById(centreId).orElseThrow();
        centre.setNom(updatedCentre.getNom());
        centre.setVille(updatedCentre.getVille());
        return centreRepository.save(centre);
    }

    @Transactional
    public void deleteEntreprise(Long entrepriseId) {
        entrepriseRepository.deleteById(entrepriseId);
    }

    @Transactional
    public void deleteCentre(Long centreId) {
        centreRepository.deleteById(centreId);
    }
}