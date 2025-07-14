package com.example.javapingpongelo.models;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AuthRequest {
    public String username;

    public String password;
}
