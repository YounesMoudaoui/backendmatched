package com.example.auto4jobs.services;

import com.example.auto4jobs.entities.User;
import com.example.auto4jobs.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
public class CVService {

    private static final Logger logger = LoggerFactory.getLogger(CVService.class);

    @Value("${file.upload-dir:./uploads/cvs}")
    private String uploadDir;
    
    private final String publicUploadDir = "./uploads/cvs-public";

    @Autowired
    private UserRepository userRepository;

    /**
     * Sauvegarde le CV d'un utilisateur et retourne les informations sur le fichier
     *
     * @param file Le fichier CV
     * @return Map contenant les informations du fichier sauvegardé
     * @throws IOException En cas d'erreur lors de la sauvegarde du fichier
     */
     
    public String getUploadDir() {
        return this.uploadDir;
    }
    
    @Transactional
    public Map<String, Object> saveCV(MultipartFile file) throws IOException {
        // Récupérer l'utilisateur authentifié
        User user = getAuthenticatedUser();

        // Vérifier le type de fichier
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        validateFileExtension(fileExtension);

        // Créer un nom de fichier unique
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

        // Créer le répertoire de destination s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            logger.info("Création du répertoire d'upload: {}", uploadPath.toAbsolutePath());
            Files.createDirectories(uploadPath);
        } else {
            logger.info("Répertoire d'upload existant: {}", uploadPath.toAbsolutePath());
        }
        
        // Créer également le répertoire public s'il n'existe pas
        Path publicPath = Paths.get(publicUploadDir);
        if (!Files.exists(publicPath)) {
            logger.info("Création du répertoire public: {}", publicPath.toAbsolutePath());
            Files.createDirectories(publicPath);
        }

        // Sauvegarder le fichier dans le répertoire principal
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Fichier sauvegardé à: {}", filePath.toAbsolutePath());
        
        // Copier également le fichier dans le répertoire public
        Path publicFilePath = publicPath.resolve(uniqueFilename);
        Files.copy(filePath, publicFilePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Fichier copié vers le répertoire public: {}", publicFilePath.toAbsolutePath());

        // Mettre à jour l'entité User avec le chemin du CV et les données binaires
        long currentTime = System.currentTimeMillis();
        user.setCvPath(uniqueFilename); // Stocker uniquement le nom du fichier, pas le chemin complet
        user.setCvFilename(uniqueFilename); // Stocker également dans le nouveau champ
        user.setCvUploadDate(currentTime);
        
        // Stocker le contenu du CV dans la base de données
        user.setCvData(file.getBytes());
        user.setCvContentType(file.getContentType());
        
        userRepository.save(user);
        logger.info("Informations du CV mises à jour pour l'utilisateur: {}", user.getEmail());

        // Retourner les informations du fichier avec URL pour prévisualisation
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("fileName", uniqueFilename);
        fileInfo.put("originalFileName", originalFilename);
        fileInfo.put("fileSize", file.getSize());
        fileInfo.put("contentType", file.getContentType());
        fileInfo.put("uploadDate", currentTime);
        fileInfo.put("filePath", "/api/users/me/cv/" + uniqueFilename);
        fileInfo.put("publicFilePath", "/static/cvs-public/" + uniqueFilename);
        fileInfo.put("storedInDatabase", true);
        
        // Préparation pour l'intégration avec Ollama lama3
        try {
            // Simuler l'extraction d'informations pour le développement
            logger.info("Préparation pour l'extraction d'informations du CV avec Ollama lama3");
            
            // Structure pour stocker les informations extraites (à implémenter avec Ollama lama3)
            Map<String, Object> extractedInfo = new HashMap<>();
            extractedInfo.put("extractionStatus", "pending");
            extractedInfo.put("cvPath", uniqueFilename);
            
            // Dans l'implémentation finale, appeler Ollama lama3 ici
            // OllamaService.extractCVInfo(filePath.toString());
            
            // Ajouter les informations extraites à la réponse
            fileInfo.put("extractedInfo", extractedInfo);
            
        } catch (Exception e) {
            logger.warn("Erreur lors de la préparation de l'extraction d'informations: {}", e.getMessage());
            // Ne pas bloquer le processus si l'extraction échoue
        }

        logger.info("CV sauvegardé avec succès: {}", filePath.toString());
        return fileInfo;
    }
    
    /**
     * Récupère le CV stocké en base de données pour un utilisateur
     *
     * @param userId L'identifiant de l'utilisateur
     * @return Les données binaires du CV ou null si aucun CV n'est trouvé
     */
    @Transactional(readOnly = true)
    public byte[] getCVDataFromDatabase(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getCvData();
        }
        return null;
    }
    
    /**
     * Récupère le CV stocké en base de données pour l'utilisateur authentifié
     *
     * @return Les données binaires du CV ou null si aucun CV n'est trouvé
     */
    @Transactional(readOnly = true)
    public byte[] getCurrentUserCVDataFromDatabase() {
        User user = getAuthenticatedUser();
        return user.getCvData();
    }
    
    /**
     * Récupère le type de contenu du CV stocké en base de données pour l'utilisateur authentifié
     *
     * @return Le type de contenu du CV ou null si aucun CV n'est trouvé
     */
    @Transactional(readOnly = true)
    public String getCurrentUserCVContentType() {
        User user = getAuthenticatedUser();
        return user.getCvContentType();
    }

    /**
     * Récupère l'utilisateur authentifié
     *
     * @return L'utilisateur authentifié
     * @throws IllegalStateException Si l'utilisateur n'est pas trouvé
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé avec l'email: " + currentUserEmail));
    }

    /**
     * Extrait l'extension d'un nom de fichier
     *
     * @param filename Le nom du fichier
     * @return L'extension du fichier
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Valide l'extension du fichier
     *
     * @param extension L'extension du fichier
     * @throws IllegalArgumentException Si l'extension n'est pas valide
     */
    private void validateFileExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            throw new IllegalArgumentException("Le fichier n'a pas d'extension");
        }
        
        extension = extension.toLowerCase();
        if (!extension.equals("pdf") && !extension.equals("doc") && !extension.equals("docx")) {
            throw new IllegalArgumentException("Seuls les fichiers PDF, DOC et DOCX sont acceptés");
        }
    }

    /**
     * Supprime le CV de l'utilisateur authentifié
     *
     * @return true si le CV a été supprimé, false si l'utilisateur n'a pas de CV
     * @throws IOException En cas d'erreur lors de la suppression du fichier
     */
    @Transactional
    public boolean deleteCurrentUserCV() throws IOException {
        // Récupérer l'utilisateur authentifié
        User user = getAuthenticatedUser();
        
        // Vérifier si l'utilisateur a un CV
        if ((user.getCvPath() == null || user.getCvPath().isEmpty()) && 
            (user.getCvFilename() == null || user.getCvFilename().isEmpty()) && 
            user.getCvData() == null) {
            logger.info("Tentative de suppression d'un CV inexistant pour l'utilisateur: {}", user.getEmail());
            return false;
        }
        
        boolean fileDeleted = false;
        
        // Supprimer le fichier du système de fichiers s'il existe
        if ((user.getCvPath() != null && !user.getCvPath().isEmpty()) || 
            (user.getCvFilename() != null && !user.getCvFilename().isEmpty())) {
            // Récupérer le chemin du fichier
            String filename = user.getCvPath() != null ? user.getCvPath() : user.getCvFilename();
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Path publicFilePath = Paths.get(publicUploadDir).resolve(filename);
            
            // Supprimer le fichier s'il existe
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    fileDeleted = true;
                    logger.info("CV supprimé du système de fichiers: {}", filePath.toAbsolutePath());
                } catch (IOException e) {
                    logger.error("Erreur lors de la suppression du fichier CV: {}", e.getMessage());
                    throw e;
                }
            } else {
                logger.warn("Le fichier CV n'existe pas sur le disque: {}", filePath.toAbsolutePath());
            }
            
            // Supprimer également la copie publique
            if (Files.exists(publicFilePath)) {
                try {
                    Files.delete(publicFilePath);
                    logger.info("CV supprimé du répertoire public: {}", publicFilePath.toAbsolutePath());
                } catch (IOException e) {
                    logger.error("Erreur lors de la suppression du fichier CV public: {}", e.getMessage());
                }
            }
        }
        
        // Mettre à jour l'entité User
        user.setCvPath(null);
        user.setCvFilename(null);
        user.setCvUploadDate(null);
        user.setCvData(null);
        user.setCvContentType(null);
        userRepository.save(user);
        logger.info("Références au CV supprimées pour l'utilisateur: {}", user.getEmail());
        
        return true;
    }

    /**
     * Vérifie si un utilisateur a un CV
     *
     * @param user L'utilisateur à vérifier
     * @return true si l'utilisateur a un CV, false sinon
     */
    public boolean hasCV(User user) {
        return (user.getCvData() != null && user.getCvData().length > 0) ||
               (user.getCvPath() != null && !user.getCvPath().isEmpty()) ||
               (user.getCvFilename() != null && !user.getCvFilename().isEmpty());
    }
    
    /**
     * Vérifie si l'utilisateur authentifié a un CV
     *
     * @return true si l'utilisateur authentifié a un CV, false sinon
     */
    public boolean currentUserHasCV() {
        User user = getAuthenticatedUser();
        return hasCV(user);
    }
    
    /**
     * Vérifie si un utilisateur a un CV
     *
     * @param userId L'identifiant de l'utilisateur
     * @return true si l'utilisateur a un CV, false sinon
     */
    public boolean userHasCV(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            return hasCV(userOpt.get());
        }
        return false;
    }
} 