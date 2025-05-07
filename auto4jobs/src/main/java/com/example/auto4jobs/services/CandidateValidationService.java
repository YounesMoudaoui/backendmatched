package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidateValidationService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getPendingCandidates() {
        return userRepository.findByIsValidatedFalseAndRoleIn(List.of("APPRENANT", "LAUREAT"));
    }

    public User validateCandidate(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("User not found"));
        if (!user.getRole().equals("APPRENANT") && !user.getRole().equals("LAUREAT")) {
            throw new IllegalStateException("Only APPRENANT or LAUREAT can be validated");
        }
        user.setIsValidated(true);
        return userRepository.save(user);
    }

    public List<User> getPendingLaureats() {
        return userRepository.findByIsValidatedFalseAndRole("LAUREAT");
    }
}