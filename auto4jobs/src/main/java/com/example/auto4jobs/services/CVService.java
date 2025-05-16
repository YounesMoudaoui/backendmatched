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

@Service
public class CVService {

    private static final Logger logger = LoggerFactory.getLogger(CVService.class);

    @Value("${file.upload-dir:uploads/cvs}")
    private String uploadDir;

    @Autowired
    private UserRepository userRepository;

    /**
     * Sauvegarde le CV d'un utilisateur et retourne les informations sur le fichier
     *
     * @param file Le fichier CV
     * @return Map contenant les informations du fichier sauvegardé
     * @throws IOException En cas d'erreur lors de la sauvegarde du fichier
     */
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
            Files.createDirectories(uploadPath);
        }

        // Sauvegarder le fichier
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Mettre à jour l'entité User avec le chemin du CV
        long currentTime = System.currentTimeMillis();
        user.setCvPath(filePath.toString());
        user.setCvUploadDate(currentTime);
        userRepository.save(user);

        // TODO: Intégrer avec Ollama lama3 pour extraire les informations du CV
        // Cette partie sera implémentée ultérieurement

        // Retourner les informations du fichier
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("fileName", uniqueFilename);
        fileInfo.put("originalFileName", originalFilename);
        fileInfo.put("fileSize", file.getSize());
        fileInfo.put("contentType", file.getContentType());
        fileInfo.put("uploadDate", currentTime);

        return fileInfo;
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
} 