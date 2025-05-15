package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.User;
// import com.example.auto4jobs.entities.Centre; // Centre is not used
import com.example.auto4jobs.repositories.UserRepository;
// import com.example.auto4jobs.repositories.CentreRepository; // CentreRepository is not used
import com.example.auto4jobs.dto.UserRegistrationDTO;
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
} 