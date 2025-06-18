package com.example.auto4jobs.repositories;

import com.example.auto4jobs.entities.MatchingResult;
import com.example.auto4jobs.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchingResultRepository extends JpaRepository<MatchingResult, Long> {
    
    /**
     * Trouve tous les résultats de matching pour un utilisateur spécifique
     * 
     * @param user L'utilisateur dont on veut récupérer les résultats de matching
     * @return Liste des résultats de matching pour cet utilisateur
     */
    List<MatchingResult> findByUser(User user);
    
    /**
     * Trouve tous les résultats de matching pour un utilisateur spécifique, triés par score de correspondance décroissant
     * 
     * @param user L'utilisateur dont on veut récupérer les résultats de matching
     * @return Liste des résultats de matching pour cet utilisateur, triés par score décroissant
     */
    List<MatchingResult> findByUserOrderByMatchScoreDesc(User user);
    
    /**
     * Trouve tous les résultats de matching pour un utilisateur spécifique créés après une certaine date
     * 
     * @param user L'utilisateur dont on veut récupérer les résultats de matching
     * @param date La date après laquelle les résultats ont été créés
     * @return Liste des résultats de matching pour cet utilisateur créés après la date spécifiée
     */
    List<MatchingResult> findByUserAndCreatedAtAfter(User user, LocalDateTime date);
    
    /**
     * Supprime tous les résultats de matching pour un utilisateur spécifique
     * 
     * @param user L'utilisateur dont on veut supprimer les résultats de matching
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MatchingResult mr WHERE mr.user = :user")
    void deleteByUser(@Param("user") User user);
} 