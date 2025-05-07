package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.Centre;
import com.example.auto4jobs.entities.Entreprise;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.CentreRepository;
import com.example.auto4jobs.repositories.EntrepriseRepository;
import com.example.auto4jobs.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

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
        User admin = userRepository.findByEmail("admin@auto4jobs.com").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("System");
            admin.setEmail("admin@auto4jobs.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setIsValidated(true);
            userRepository.save(admin);
        } else {
            // Force le mot de passe à admin123 à chaque démarrage pour debug
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setIsValidated(true);
            userRepository.save(admin);
        }

        // Créer un compte directeur exécutif par défaut s'il n'existe pas
        if (!userRepository.findByEmail("directeur@auto4jobs.com").isPresent()) {
            User directeur = new User();
            directeur.setFirstName("Directeur");
            directeur.setLastName("Exécutif");
            directeur.setEmail("directeur@auto4jobs.com");
            directeur.setPassword(passwordEncoder.encode("directeur123"));
            directeur.setRole("DIRECTEUR_EXECUTIF");
            directeur.setIsValidated(true);
            userRepository.save(directeur);
        }
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        String adminEmail = "admin@auto4jobs.com";
        String adminPassword = "admin123";

        userRepository.findByEmail(adminEmail).ifPresentOrElse(
            user -> {
                // Mettre à jour le mot de passe et la validation si l'admin existe déjà
                user.setPassword(passwordEncoder.encode(adminPassword));
                user.setIsValidated(true);
                userRepository.save(user);
            },
            () -> {
                // Créer un nouvel admin s'il n'existe pas
                User admin = new User();
                admin.setFirstName("Admin");
                admin.setLastName("System");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                admin.setIsValidated(true);
                userRepository.save(admin);
            }
        );
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole().equals("APPRENANT") || user.getRole().equals("LAUREAT")) {
            user.setIsValidated(false); // Candidates require validation
        } else {
            user.setIsValidated(true); // Other roles are validated by default
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public Entreprise createEntreprise(Entreprise entreprise) {
        return entrepriseRepository.save(entreprise);
    }

    public Centre createCentre(Centre centre) {
        try {
            System.out.println("Tentative de création du centre: " + centre);
            System.out.println("Ville: " + centre.getVille());
            System.out.println("Nom: " + centre.getNom());
            System.out.println("Email Domain: " + centre.getEmailDomain());
            
            if (centre.getVille() == null || centre.getVille().trim().isEmpty()) {
                System.out.println("Erreur: La ville est vide");
                throw new IllegalArgumentException("La ville est obligatoire");
            }
            if (centre.getNom() == null || centre.getNom().trim().isEmpty()) {
                System.out.println("Erreur: Le nom est vide");
                throw new IllegalArgumentException("Le nom est obligatoire");
            }
            
            Centre savedCentre = centreRepository.save(centre);
            System.out.println("Centre créé avec succès: " + savedCentre);
            return savedCentre;
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du centre: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public User assignResponsableToCentre(Long userId, Long centreId) {
        User user = userRepository.findById(userId).orElseThrow();
        Centre centre = centreRepository.findById(centreId).orElseThrow();
        user.setCentre(centre);
        user.setRole("RESPONSABLE_CENTRE");
        return userRepository.save(user);
    }

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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<Entreprise> getAllEntreprises() {
        return entrepriseRepository.findAll();
    }

    public List<Centre> getAllCentres() {
        return centreRepository.findAll();
    }

    public List<User> getAllRecruiters() {
        return userRepository.findByRole("RECRUTEUR");
    }

    public User updateUser(Long userId, User updatedUser) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setEmail(updatedUser.getEmail());
        user.setPhone(updatedUser.getPhone());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepository.save(user);
    }

    public Entreprise updateEntreprise(Long entrepriseId, Entreprise updatedEntreprise) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId).orElseThrow();
        entreprise.setNom(updatedEntreprise.getNom());
        entreprise.setLogoUrl(updatedEntreprise.getLogoUrl());
        entreprise.setCareerPageUrl(updatedEntreprise.getCareerPageUrl());
        return entrepriseRepository.save(entreprise);
    }

    public Centre updateCentre(Long centreId, Centre updatedCentre) {
        Centre centre = centreRepository.findById(centreId).orElseThrow();
        centre.setNom(updatedCentre.getNom());
        centre.setVille(updatedCentre.getVille());
        return centreRepository.save(centre);
    }

    public void deleteEntreprise(Long entrepriseId) {
        entrepriseRepository.deleteById(entrepriseId);
    }

    public void deleteCentre(Long centreId) {
        centreRepository.deleteById(centreId);
    }
}