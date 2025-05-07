package com.example.auto4jobs.dto;

import lombok.Data;

@Data
public class UserRegistrationDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String role;
} 