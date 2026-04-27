package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
public class User {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    public void setPassword(String rawPassword) {
        this.password = PASSWORD_ENCODER.encode(rawPassword);
    }

    public boolean checkPassword(String rawPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, this.password);
    }
}
