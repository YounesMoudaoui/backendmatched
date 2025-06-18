package com.example.auto4jobs.controllers;

import com.example.auto4jobs.dto.UserRegistrationDTO;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDTO registrationDTO) {
        try {
            if (!registrationDTO.getRole().equals("APPRENANT") && !registrationDTO.getRole().equals("LAUREAT")) {
                return ResponseEntity.badRequest().body("Seuls les rôles APPRENANT ou LAUREAT sont autorisés.");
            }
            userService.registerUser(registrationDTO);
            return ResponseEntity.ok("Utilisateur enregistré avec succès.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de l'inscription.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginData,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        try {
            String email = loginData.get("email");
            String password = loginData.get("password");

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            
            org.springframework.security.core.context.SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            
            securityContextRepository.saveContext(context, request, response);

            User user = userService.findByEmail(email);
            if (user == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Utilisateur non trouvé après authentification.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("status", "success");
            responseMap.put("role", user.getRole());
            responseMap.put("email", user.getEmail());
            responseMap.put("firstName", user.getFirstName());
            responseMap.put("lastName", user.getLastName());
            
            return ResponseEntity.ok(responseMap);
        } catch (org.springframework.security.core.AuthenticationException e) {
            e.printStackTrace(); 
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Email ou mot de passe incorrect.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) { 
            e.printStackTrace(); 
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Erreur interne du serveur lors de la connexion.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/auth/validate-token")
    public ResponseEntity<?> validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            if (user != null) {
                Map<String, String> responseData = new HashMap<>();
                responseData.put("status", "success");
                responseData.put("role", user.getRole());
                responseData.put("email", user.getEmail());
                responseData.put("firstName", user.getFirstName());
                responseData.put("lastName", user.getLastName());
                return ResponseEntity.ok(responseData);
            }
        }
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", "Session invalide ou utilisateur non authentifié.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            // Garder ce commentaire en phase de développement
            // new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        session.invalidate();
        SecurityContextHolder.clearContext(); 
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("status", "success");
        responseBody.put("message", "Déconnexion réussie.");
        return ResponseEntity.ok(responseBody);
    }
}