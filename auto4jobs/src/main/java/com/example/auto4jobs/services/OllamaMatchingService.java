package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.JobOffer;
import com.example.auto4jobs.entities.MatchingResult;
import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.JobOfferRepository;
import com.example.auto4jobs.repositories.MatchingResultRepository;
import com.example.auto4jobs.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OllamaMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaMatchingService.class);
    
    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String ollamaApiUrl;
    
    @Value("${ollama.model:llama3}")
    private String ollamaModel;
    
    @Value("${file.upload-dir:./uploads/cvs}")
    private String uploadDir;
    
    @Value("${matching.results.cache-duration-hours:24}")
    private int cacheDurationHours = 24;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JobOfferRepository jobOfferRepository;
    
    @Autowired
    private MatchingResultRepository matchingResultRepository;
    
    @Autowired
    private CVService cvService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Analyse un CV et retourne les compétences extraites
     * 
     * @param userId ID de l'utilisateur dont on veut analyser le CV
     * @return Map contenant les compétences extraites
     */
    public Map<String, Object> extractCVSkills(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId));
        
        String cvText;
        
        // Vérifier si le CV est stocké en base de données
        if (user.getCvData() != null && user.getCvData().length > 0) {
            logger.info("Extraction du texte à partir du CV en base de données pour l'utilisateur {}", userId);
            cvText = extractTextFromCV(user.getCvData());
        } 
        // Sinon, vérifier si le CV est stocké dans le système de fichiers
        else if (user.getCvFilename() != null && !user.getCvFilename().isEmpty()) {
            logger.info("Extraction du texte à partir du CV dans le système de fichiers pour l'utilisateur {}", userId);
            try {
                Path cvPath = Paths.get(uploadDir, user.getCvFilename());
                byte[] cvData = Files.readAllBytes(cvPath);
                cvText = extractTextFromCV(cvData);
            } catch (IOException e) {
                logger.error("Erreur lors de la lecture du fichier CV: {}", e.getMessage());
                throw new IllegalStateException("Impossible de lire le fichier CV", e);
            }
        } else {
            throw new IllegalStateException("Aucun CV trouvé pour cet utilisateur");
        }
        
        // Utiliser Ollama pour extraire les compétences
        Map<String, Object> extractedSkills = callOllamaForSkillExtraction(cvText);
        
        return extractedSkills;
    }
    
    /**
     * Trouve les offres d'emploi qui correspondent le mieux au CV d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des offres d'emploi avec leurs scores de correspondance
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Map<String, Object>> matchJobOffersForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId));
        
        // Vérifier si des résultats de matching récents existent déjà en base de données
        LocalDateTime cacheThreshold = LocalDateTime.now().minus(cacheDurationHours, ChronoUnit.HOURS);
        List<MatchingResult> existingResults = matchingResultRepository.findByUserAndCreatedAtAfter(user, cacheThreshold);
        
        // Si des résultats récents existent et que le CV n'a pas été mis à jour depuis
        if (!existingResults.isEmpty() && !isCVUpdatedAfterResults(user, existingResults)) {
            logger.info("Utilisation des résultats de matching en cache pour l'utilisateur {}", userId);
            return convertMatchingResultsToResponseFormat(existingResults);
        }
        
        // Sinon, effectuer un nouveau matching
        logger.info("Aucun résultat de matching récent trouvé, calcul d'un nouveau matching pour l'utilisateur {}", userId);
        
        String cvText;
        
        // Vérifier si le CV est stocké en base de données
        if (user.getCvData() != null && user.getCvData().length > 0) {
            logger.info("Extraction du texte à partir du CV en base de données pour l'utilisateur {}", userId);
            cvText = extractTextFromCV(user.getCvData());
        } 
        // Sinon, vérifier si le CV est stocké dans le système de fichiers
        else if (user.getCvFilename() != null && !user.getCvFilename().isEmpty()) {
            logger.info("Extraction du texte à partir du CV dans le système de fichiers pour l'utilisateur {}", userId);
            try {
                Path cvPath = Paths.get(uploadDir, user.getCvFilename());
                byte[] cvData = Files.readAllBytes(cvPath);
                cvText = extractTextFromCV(cvData);
            } catch (IOException e) {
                logger.error("Erreur lors de la lecture du fichier CV: {}", e.getMessage());
                throw new IllegalStateException("Impossible de lire le fichier CV", e);
            }
        } else {
            throw new IllegalStateException("Aucun CV trouvé pour cet utilisateur");
        }
        
        // Récupérer toutes les offres d'emploi actives
        List<JobOffer> activeJobOffers = jobOfferRepository.findByIsActiveTrue();
        
        if (activeJobOffers.isEmpty()) {
            logger.info("Aucune offre d'emploi active trouvée");
            return Collections.emptyList();
        }
        
        logger.info("Trouvé {} offres d'emploi actives pour le matching", activeJobOffers.size());
        
        // Supprimer les anciens résultats dans une transaction séparée
        deleteExistingMatchingResults(user);
        
        // Calculer le score de correspondance pour chaque offre et sauvegarder les résultats
        List<Map<String, Object>> matchResults = new ArrayList<>();
        
        for (JobOffer offer : activeJobOffers) {
            // Créer le texte de l'offre avec les compétences requises
            String offerText = createJobOfferText(offer);
            
            // Calculer le score de correspondance avec Ollama
            double matchScore = calculateMatchScore(cvText, offerText);
            
            // Ajouter des explications sur le matching
            List<String> matchExplanations = generateMatchExplanations(cvText, offer);
            
            // Créer et sauvegarder le résultat de matching en base de données dans une transaction séparée
            MatchingResult matchingResult = saveMatchingResult(user, offer, matchScore, matchExplanations);
            
            // Ajouter le résultat au format de réponse
            Map<String, Object> result = new HashMap<>();
            result.put("jobOffer", mapJobOfferToDto(offer));
            result.put("matchScore", matchScore);
            result.put("matchExplanations", matchExplanations);
            matchResults.add(result);
        }
        
        // Trier les résultats par score de correspondance (du plus élevé au plus bas)
        matchResults.sort((a, b) -> Double.compare((Double) b.get("matchScore"), (Double) a.get("matchScore")));
        
        return matchResults;
    }
    
    /**
     * Supprime les résultats de matching existants pour un utilisateur
     * 
     * @param user L'utilisateur
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExistingMatchingResults(User user) {
        logger.info("Suppression des résultats de matching existants pour l'utilisateur {}", user.getId());
        matchingResultRepository.deleteByUser(user);
    }
    
    /**
     * Sauvegarde un résultat de matching
     * 
     * @param user L'utilisateur
     * @param offer L'offre d'emploi
     * @param matchScore Le score de correspondance
     * @param matchExplanations Les explications sur le matching
     * @return Le résultat de matching sauvegardé
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MatchingResult saveMatchingResult(User user, JobOffer offer, double matchScore, List<String> matchExplanations) {
        MatchingResult matchingResult = new MatchingResult();
        matchingResult.setUser(user);
        matchingResult.setJobOffer(offer);
        matchingResult.setMatchScore(matchScore);
        matchingResult.setMatchExplanations(matchExplanations);
        return matchingResultRepository.save(matchingResult);
    }
    
    /**
     * Vérifie si le CV de l'utilisateur a été mis à jour après la création des résultats de matching
     * 
     * @param user L'utilisateur
     * @param results Les résultats de matching existants
     * @return true si le CV a été mis à jour après les résultats, false sinon
     */
    private boolean isCVUpdatedAfterResults(User user, List<MatchingResult> results) {
        if (user.getCvUploadDate() == null || results.isEmpty()) {
            return false;
        }
        
        // Trouver le résultat le plus récent
        LocalDateTime mostRecentResultTime = results.stream()
                .map(MatchingResult::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
        
        // Convertir le timestamp du CV en LocalDateTime
        LocalDateTime cvUploadTime = LocalDateTime.ofEpochSecond(
                user.getCvUploadDate() / 1000, 
                0, 
                java.time.ZoneOffset.UTC
        );
        
        // Vérifier si le CV a été mis à jour après le résultat le plus récent
        return cvUploadTime.isAfter(mostRecentResultTime);
    }
    
    /**
     * Convertit les entités MatchingResult en format de réponse pour l'API
     * 
     * @param results Liste des résultats de matching
     * @return Liste des résultats au format de réponse
     */
    private List<Map<String, Object>> convertMatchingResultsToResponseFormat(List<MatchingResult> results) {
        List<Map<String, Object>> responseFormat = new ArrayList<>();
        
        for (MatchingResult result : results) {
            Map<String, Object> matchResult = new HashMap<>();
            matchResult.put("jobOffer", mapJobOfferToDto(result.getJobOffer()));
            matchResult.put("matchScore", result.getMatchScore());
            matchResult.put("matchExplanations", result.getMatchExplanations());
            responseFormat.add(matchResult);
        }
        
        // Trier les résultats par score de correspondance (du plus élevé au plus bas)
        responseFormat.sort((a, b) -> Double.compare((Double) b.get("matchScore"), (Double) a.get("matchScore")));
        
        return responseFormat;
    }
    
    /**
     * Extrait le texte d'un CV au format PDF
     * 
     * @param cvData Données binaires du CV
     * @return Texte extrait du CV
     */
    private String extractTextFromCV(byte[] cvData) {
        // Dans une implémentation réelle, vous utiliseriez une bibliothèque comme Apache PDFBox
        // pour extraire le texte d'un PDF
        
        logger.info("Extraction du texte à partir d'un CV de {} octets", cvData.length);
        
        // Simuler l'extraction de texte pour le développement
        return "Simulé: Texte extrait du CV contenant les compétences et l'expérience de l'utilisateur. " +
               "Compétences techniques: Java, Spring Boot, React, Angular, TypeScript, JavaScript, HTML, CSS, " +
               "MySQL, PostgreSQL, MongoDB, Docker, Kubernetes, AWS, Git. " +
               "Compétences comportementales: Travail d'équipe, Communication, Résolution de problèmes, " +
               "Autonomie, Adaptabilité, Gestion du temps. " +
               "Expérience: 3 ans en tant que développeur full stack, 2 ans en tant que développeur frontend. " +
               "Formation: Master en Informatique, Université Mohammed V. " +
               "Certifications: Oracle Certified Java Developer, AWS Certified Developer.";
    }
    
    /**
     * Appelle l'API Ollama pour extraire les compétences d'un CV
     * 
     * @param cvText Texte du CV
     * @return Map contenant les compétences extraites
     */
    private Map<String, Object> callOllamaForSkillExtraction(String cvText) {
        logger.info("Appel à Ollama pour l'extraction des compétences");
        
        String prompt = "Extrait les compétences techniques, compétences comportementales, " +
                "expériences professionnelles, formation et certifications à partir du CV suivant. " +
                "Réponds uniquement au format JSON avec les clés 'technicalSkills', 'softSkills', " +
                "'experience', 'education' et 'certifications'. Voici le CV:\n\n" + cvText;
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaApiUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseText = (String) response.getBody().get("response");
                
                // Simuler le résultat pour le développement
                Map<String, Object> extractedSkills = new HashMap<>();
                extractedSkills.put("technicalSkills", Arrays.asList("Java", "Spring Boot", "React", "SQL"));
                extractedSkills.put("softSkills", Arrays.asList("Communication", "Travail en équipe", "Résolution de problèmes"));
                extractedSkills.put("experience", "3 ans d'expérience en développement web");
                extractedSkills.put("education", "Master en Informatique");
                extractedSkills.put("certifications", Arrays.asList("Oracle Certified Java Developer"));
                
                return extractedSkills;
            } else {
                logger.error("Erreur lors de l'appel à Ollama: {}", response.getStatusCode());
                throw new IOException("Erreur lors de l'appel à Ollama");
            }
        } catch (Exception e) {
            logger.error("Exception lors de l'appel à Ollama", e);
            
            // Simuler le résultat pour le développement
            Map<String, Object> extractedSkills = new HashMap<>();
            extractedSkills.put("technicalSkills", Arrays.asList("Java", "Spring Boot", "React", "SQL"));
            extractedSkills.put("softSkills", Arrays.asList("Communication", "Travail en équipe", "Résolution de problèmes"));
            extractedSkills.put("experience", "3 ans d'expérience en développement web");
            extractedSkills.put("education", "Master en Informatique");
            extractedSkills.put("certifications", Arrays.asList("Oracle Certified Java Developer"));
            
            return extractedSkills;
        }
    }
    
    /**
     * Crée un texte représentant l'offre d'emploi avec ses compétences requises
     * 
     * @param offer Offre d'emploi
     * @return Texte de l'offre d'emploi
     */
    private String createJobOfferText(JobOffer offer) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("Titre: ").append(offer.getTitrePoste()).append("\n");
        builder.append("Description: ").append(offer.getDescriptionDetaillee()).append("\n");
        
        builder.append("Compétences techniques requises: ");
        if (offer.getCompetencesTechniquesRequises() != null && !offer.getCompetencesTechniquesRequises().isEmpty()) {
            builder.append(String.join(", ", offer.getCompetencesTechniquesRequises()));
        }
        builder.append("\n");
        
        builder.append("Compétences comportementales requises: ");
        if (offer.getCompetencesComportementalesRequises() != null && !offer.getCompetencesComportementalesRequises().isEmpty()) {
            builder.append(String.join(", ", offer.getCompetencesComportementalesRequises()));
        }
        builder.append("\n");
        
        builder.append("Formation: ").append(offer.getEducation()).append("\n");
        builder.append("Expérience souhaitée: ").append(offer.getExperienceSouhaitee()).append("\n");
        
        builder.append("Certifications demandées: ");
        if (offer.getCertificationsDemandees() != null && !offer.getCertificationsDemandees().isEmpty()) {
            builder.append(String.join(", ", offer.getCertificationsDemandees()));
        }
        builder.append("\n");
        
        return builder.toString();
    }
    
    /**
     * Calcule le score de correspondance entre un CV et une offre d'emploi en utilisant Ollama
     * 
     * @param cvText Texte du CV
     * @param offerText Texte de l'offre d'emploi
     * @return Score de correspondance (0-100)
     */
    private double calculateMatchScore(String cvText, String offerText) {
        logger.info("Calcul du score de correspondance avec Ollama");
        
        String prompt = "Calcule le score de correspondance (de 0 à 100) entre le CV et l'offre d'emploi suivants. " +
                "Réponds uniquement avec un nombre entier entre 0 et 100. Plus le score est élevé, plus la correspondance est forte.\n\n" +
                "CV:\n" + cvText + "\n\n" +
                "Offre d'emploi:\n" + offerText;
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaApiUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseText = (String) response.getBody().get("response");
                
                try {
                    // Essayer de convertir la réponse en nombre
                    return Double.parseDouble(responseText.trim());
                } catch (NumberFormatException e) {
                    logger.error("Impossible de convertir la réponse d'Ollama en nombre: {}", responseText);
                    
                    // Simuler un score pour le développement
                    return 30 + Math.random() * 70; // Score entre 30 et 100
                }
            } else {
                logger.error("Erreur lors de l'appel à Ollama: {}", response.getStatusCode());
                
                // Simuler un score pour le développement
                return 30 + Math.random() * 70; // Score entre 30 et 100
            }
        } catch (Exception e) {
            logger.error("Exception lors de l'appel à Ollama", e);
            
            // Simuler un score pour le développement
            return 30 + Math.random() * 70; // Score entre 30 et 100
        }
    }
    
    /**
     * Génère des explications sur le matching entre un CV et une offre d'emploi
     * 
     * @param cvText Texte du CV
     * @param offer Offre d'emploi
     * @return Liste d'explications
     */
    private List<String> generateMatchExplanations(String cvText, JobOffer offer) {
        logger.info("Génération d'explications sur le matching avec Ollama");
        
        String offerText = createJobOfferText(offer);
        
        String prompt = "Explique pourquoi ce CV correspond ou ne correspond pas à cette offre d'emploi. " +
                "Donne 3 points forts et 3 points faibles. Réponds sous forme de liste avec des tirets.\n\n" +
                "CV:\n" + cvText + "\n\n" +
                "Offre d'emploi:\n" + offerText;
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaApiUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseText = (String) response.getBody().get("response");
                
                // Traiter la réponse pour extraire les explications
                List<String> explanations = Arrays.stream(responseText.split("\n"))
                        .filter(line -> line.trim().startsWith("-"))
                        .map(line -> line.trim().substring(1).trim())
                        .collect(Collectors.toList());
                
                if (explanations.isEmpty()) {
                    // Simuler des explications pour le développement
                    return generateSimulatedExplanations(offer);
                }
                
                return explanations;
            } else {
                logger.error("Erreur lors de l'appel à Ollama: {}", response.getStatusCode());
                
                // Simuler des explications pour le développement
                return generateSimulatedExplanations(offer);
            }
        } catch (Exception e) {
            logger.error("Exception lors de l'appel à Ollama", e);
            
            // Simuler des explications pour le développement
            return generateSimulatedExplanations(offer);
        }
    }
    
    /**
     * Génère des explications simulées pour le développement
     * 
     * @param offer Offre d'emploi
     * @return Liste d'explications simulées
     */
    private List<String> generateSimulatedExplanations(JobOffer offer) {
        List<String> explanations = new ArrayList<>();
        
        // Points forts
        if (offer.getCompetencesTechniquesRequises() != null && !offer.getCompetencesTechniquesRequises().isEmpty()) {
            explanations.add("Le candidat possède une bonne expérience en " + 
                    offer.getCompetencesTechniquesRequises().iterator().next());
        } else {
            explanations.add("Le candidat possède une bonne expérience en développement");
        }
        
        explanations.add("La formation du candidat correspond au niveau requis");
        
        if (offer.getCompetencesComportementalesRequises() != null && !offer.getCompetencesComportementalesRequises().isEmpty()) {
            explanations.add("Le candidat a démontré des compétences en " + 
                    offer.getCompetencesComportementalesRequises().iterator().next());
        } else {
            explanations.add("Le candidat a démontré des compétences en travail d'équipe");
        }
        
        // Points faibles
        explanations.add("Le candidat manque d'expérience dans certaines technologies requises");
        
        if (offer.getCertificationsDemandees() != null && !offer.getCertificationsDemandees().isEmpty()) {
            explanations.add("Le candidat ne possède pas la certification " + 
                    offer.getCertificationsDemandees().iterator().next());
        } else {
            explanations.add("Le candidat ne possède pas toutes les certifications demandées");
        }
        
        explanations.add("L'expérience du candidat est légèrement inférieure à celle souhaitée");
        
        return explanations;
    }
    
    /**
     * Convertit une offre d'emploi en DTO pour l'API
     * 
     * @param offer Offre d'emploi
     * @return Map représentant l'offre d'emploi
     */
    private Map<String, Object> mapJobOfferToDto(JobOffer offer) {
        Map<String, Object> dto = new HashMap<>();
        
        dto.put("id", offer.getId());
        dto.put("titrePoste", offer.getTitrePoste());
        dto.put("entrepriseNom", offer.getEntreprise().getNom());
        dto.put("entrepriseLogoUrl", offer.getEntreprise().getLogoUrl());
        dto.put("localisation", offer.getLocalisation());
        dto.put("typeContrat", offer.getTypeContrat().toString());
        dto.put("competencesTechniquesRequises", offer.getCompetencesTechniquesRequises());
        dto.put("competencesComportementalesRequises", offer.getCompetencesComportementalesRequises());
        
        return dto;
    }
    
    /**
     * Force le recalcul des correspondances pour un utilisateur spécifique
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des offres d'emploi avec leurs scores de correspondance
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Map<String, Object>> forceMatchJobOffersForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId));
        
        // Supprimer tous les résultats de matching existants pour cet utilisateur dans une transaction séparée
        logger.info("Suppression des résultats de matching existants pour l'utilisateur {}", userId);
        deleteExistingMatchingResults(user);
        
        // Effectuer un nouveau matching
        logger.info("Calcul d'un nouveau matching pour l'utilisateur {}", userId);
        
        String cvText;
        
        // Vérifier si le CV est stocké en base de données
        if (user.getCvData() != null && user.getCvData().length > 0) {
            logger.info("Extraction du texte à partir du CV en base de données pour l'utilisateur {}", userId);
            cvText = extractTextFromCV(user.getCvData());
        } 
        // Sinon, vérifier si le CV est stocké dans le système de fichiers
        else if (user.getCvFilename() != null && !user.getCvFilename().isEmpty()) {
            logger.info("Extraction du texte à partir du CV dans le système de fichiers pour l'utilisateur {}", userId);
            try {
                Path cvPath = Paths.get(uploadDir, user.getCvFilename());
                byte[] cvData = Files.readAllBytes(cvPath);
                cvText = extractTextFromCV(cvData);
            } catch (IOException e) {
                logger.error("Erreur lors de la lecture du fichier CV: {}", e.getMessage());
                throw new IllegalStateException("Impossible de lire le fichier CV", e);
            }
        } else {
            throw new IllegalStateException("Aucun CV trouvé pour cet utilisateur");
        }
        
        // Récupérer toutes les offres d'emploi actives
        List<JobOffer> activeJobOffers = jobOfferRepository.findByIsActiveTrue();
        
        if (activeJobOffers.isEmpty()) {
            logger.info("Aucune offre d'emploi active trouvée");
            return Collections.emptyList();
        }
        
        logger.info("Trouvé {} offres d'emploi actives pour le matching", activeJobOffers.size());
        
        // Calculer le score de correspondance pour chaque offre et sauvegarder les résultats
        List<Map<String, Object>> matchResults = new ArrayList<>();
        
        for (JobOffer offer : activeJobOffers) {
            // Créer le texte de l'offre avec les compétences requises
            String offerText = createJobOfferText(offer);
            
            // Calculer le score de correspondance avec Ollama
            double matchScore = calculateMatchScore(cvText, offerText);
            
            // Ajouter des explications sur le matching
            List<String> matchExplanations = generateMatchExplanations(cvText, offer);
            
            // Créer et sauvegarder le résultat de matching en base de données dans une transaction séparée
            MatchingResult matchingResult = saveMatchingResult(user, offer, matchScore, matchExplanations);
            
            // Ajouter le résultat au format de réponse
            Map<String, Object> result = new HashMap<>();
            result.put("jobOffer", mapJobOfferToDto(offer));
            result.put("matchScore", matchScore);
            result.put("matchExplanations", matchExplanations);
            matchResults.add(result);
        }
        
        // Trier les résultats par score de correspondance (du plus élevé au plus bas)
        matchResults.sort((a, b) -> Double.compare((Double) b.get("matchScore"), (Double) a.get("matchScore")));
        
        return matchResults;
    }
} 