package com.tricia.smartmentor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String grade;
    private String school;
    private String avatarUrl;
}
