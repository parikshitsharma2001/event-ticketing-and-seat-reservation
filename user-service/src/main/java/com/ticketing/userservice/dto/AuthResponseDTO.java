package com.ticketing.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private UserDTO user;
    
    public AuthResponseDTO(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }
}
