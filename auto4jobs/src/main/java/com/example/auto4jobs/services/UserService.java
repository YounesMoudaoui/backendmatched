package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.User;
// import com.example.auto4jobs.entities.Centre; // Centre is not used
import com.example.auto4jobs.repositories.UserRepository;
// import com.example.auto4jobs.repositories.CentreRepository; // CentreRepository is not used
import com.example.auto4jobs.dto.UserRegistrationDTO;
import com.example.auto4jobs.dto.UserProfileDTO;
import com.example.auto4jobs.dto.EntrepriseSlimDTO;
import com.example.auto4jobs.entities.Entreprise; // Required for mapping
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // For read-only transaction

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private CentreRepository centreRepository; // No longer used

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDTO registrationDTO) {
        // Existing email uniqueness check
        if (userRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        User user = new User();
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setPhone(registrationDTO.getPhone());
        user.setRole(registrationDTO.getRole());

        // Simplified logic for setting isValidated
        user.setIsValidated(!registrationDTO.getRole().equals("APPRENANT"));

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getCurrentUserProfile() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database, which should not happen."));

        Set<EntrepriseSlimDTO> entrepriseDTOs = Collections.emptySet();
        if ("RECRUTEUR".equals(user.getRole()) && user.getEntreprises() != null) {
            entrepriseDTOs = user.getEntreprises().stream()
                    .map(entreprise -> new EntrepriseSlimDTO(entreprise.getId(), entreprise.getNom()))
                    .collect(Collectors.toSet());
        }

        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isIntermediateRecruiter(),
                entrepriseDTOs
        );
    }
} 