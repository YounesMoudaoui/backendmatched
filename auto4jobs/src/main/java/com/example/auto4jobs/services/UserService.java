package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.User;
// import com.example.auto4jobs.entities.Centre; // Centre is not used
import com.example.auto4jobs.repositories.UserRepository;
// import com.example.auto4jobs.repositories.CentreRepository; // CentreRepository is not used
import com.example.auto4jobs.dto.UserRegistrationDTO;
import com.example.auto4jobs.dto.UserProfileDTO;
import com.example.auto4jobs.dto.UserProfileUpdateDTO;
import com.example.auto4jobs.dto.EntrepriseSlimDTO;
import com.example.auto4jobs.entities.Entreprise; // Required for mapping
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // For read-only transaction
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private CentreRepository centreRepository; // No longer used

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${file.upload-dir:./uploads/cvs}")
    private String cvUploadDir;

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

        boolean hasCvInDatabase = user.getCvData() != null && user.getCvData().length > 0;
        
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isIntermediateRecruiter(),
                entrepriseDTOs,
                user.getCvPath(),
                user.getCvUploadDate(),
                hasCvInDatabase,
                user.getCvContentType()
        );
    }

    /**
     * Récupère les informations du CV de l'utilisateur actuellement connecté
     * 
     * @return Map contenant les informations du CV
     */
    public Map<String, Object> getCurrentUserCVInfo() {
        User user = getCurrentUser();
        Map<String, Object> cvInfo = new HashMap<>();
        
        boolean hasCvInFile = user.getCvPath() != null && !user.getCvPath().isEmpty();
        boolean hasCvInDatabase = user.getCvData() != null && user.getCvData().length > 0;
        
        cvInfo.put("hasCvInFile", hasCvInFile);
        cvInfo.put("hasCvInDatabase", hasCvInDatabase);
        
        if (hasCvInFile) {
            String filename = user.getCvPath();
            cvInfo.put("fileName", filename);
            cvInfo.put("uploadDate", user.getCvUploadDate());
            cvInfo.put("filePath", "/api/users/me/cv/" + filename);
            
            // Déterminer le type de contenu
            String contentType = "application/octet-stream";
            if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.toLowerCase().endsWith(".doc")) {
                contentType = "application/msword";
            } else if (filename.toLowerCase().endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            cvInfo.put("contentType", contentType);
            
            // Vérifier si le fichier existe réellement
            try {
                Path filePath = Paths.get(cvUploadDir).resolve(filename);
                boolean fileExists = Files.exists(filePath) && Files.isReadable(filePath);
                cvInfo.put("fileExists", fileExists);
                if (fileExists) {
                    cvInfo.put("fileSize", Files.size(filePath));
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification du fichier CV: {}", e.getMessage());
                cvInfo.put("fileExists", false);
            }
        }
        
        if (hasCvInDatabase) {
            cvInfo.put("databaseCvSize", user.getCvData().length);
            cvInfo.put("databaseCvContentType", user.getCvContentType());
            cvInfo.put("databaseCvPath", "/api/users/me/cv-from-db");
            cvInfo.put("uploadDate", user.getCvUploadDate());
        }
        
        cvInfo.put("hasCV", hasCvInFile || hasCvInDatabase);
        
        return cvInfo;
    }

    /**
     * Met à jour le profil de l'utilisateur actuellement connecté
     * 
     * @param profileUpdateDTO DTO contenant les informations à mettre à jour
     * @return Le profil utilisateur mis à jour
     * @throws IllegalStateException Si l'utilisateur n'est pas trouvé
     * @throws IllegalArgumentException Si l'email est déjà utilisé par un autre utilisateur
     */
    @Transactional
    public UserProfileDTO updateCurrentUserProfile(UserProfileUpdateDTO profileUpdateDTO) {
        User currentUser = getCurrentUser();
        
        // Vérifier si l'email est déjà utilisé par un autre utilisateur
        if (!currentUser.getEmail().equals(profileUpdateDTO.getEmail())) {
            userRepository.findByEmail(profileUpdateDTO.getEmail())
                .ifPresent(user -> {
                    if (!user.getId().equals(currentUser.getId())) {
                        throw new IllegalArgumentException("Cet email est déjà utilisé par un autre utilisateur.");
                    }
                });
        }
        
        // Mettre à jour les informations de l'utilisateur
        currentUser.setFirstName(profileUpdateDTO.getFirstName());
        currentUser.setLastName(profileUpdateDTO.getLastName());
        currentUser.setEmail(profileUpdateDTO.getEmail());
        if (profileUpdateDTO.getPhone() != null) {
            currentUser.setPhone(profileUpdateDTO.getPhone());
        }
        
        // Sauvegarder les modifications
        User updatedUser = userRepository.save(currentUser);
        
        // Retourner le profil mis à jour
        Set<EntrepriseSlimDTO> entrepriseDTOs = Collections.emptySet();
        if ("RECRUTEUR".equals(updatedUser.getRole()) && updatedUser.getEntreprises() != null) {
            entrepriseDTOs = updatedUser.getEntreprises().stream()
                    .map(entreprise -> new EntrepriseSlimDTO(entreprise.getId(), entreprise.getNom()))
                    .collect(Collectors.toSet());
        }

        return new UserProfileDTO(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                updatedUser.getRole(),
                updatedUser.isIntermediateRecruiter(),
                entrepriseDTOs,
                updatedUser.getCvPath(),
                updatedUser.getCvUploadDate()
        );
    }

    /**
     * Récupère l'utilisateur actuellement connecté
     * 
     * @return L'utilisateur connecté
     * @throws IllegalStateException Si l'utilisateur n'est pas trouvé
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé avec l'email: " + currentUserEmail));
    }
} 