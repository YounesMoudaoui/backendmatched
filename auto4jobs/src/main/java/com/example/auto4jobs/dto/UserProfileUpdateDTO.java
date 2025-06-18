package com.example.auto4jobs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO pour la mise à jour du profil utilisateur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
} 