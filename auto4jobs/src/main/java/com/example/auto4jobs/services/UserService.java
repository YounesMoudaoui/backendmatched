package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.entities.Centre;
import com.example.auto4jobs.repositories.UserRepository;
import com.example.auto4jobs.repositories.CentreRepository;
import com.example.auto4jobs.dto.UserRegistrationDTO;
import com.example.auto4jobs.validation.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CentreRepository centreRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDTO registrationDTO) {
        // Validate email using custom validator
        if (!EmailValidator.isValidEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Format d'email invalide.");
        }

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

        // Extract email domain
        String emailDomain = EmailValidator.getEmailDomain(registrationDTO.getEmail());
        Centre centre = emailDomain != null ? 
            centreRepository.findByEmailDomain(emailDomain).orElse(null) : null;
        
        if (centre != null) {
            user.setCentre(centre);
            if (registrationDTO.getRole().equals("APPRENANT")) {
                user.setIsValidated(false);
            } else {
                user.setIsValidated(true);
            }
        } else {
            if (registrationDTO.getRole().equals("APPRENANT")) {
                user.setIsValidated(false);
            } else {
                user.setIsValidated(true);
            }
        }

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));
    }
} 