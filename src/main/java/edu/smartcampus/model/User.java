
package edu.smartcampus.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 190)
    public String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    public String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    public String fullName;

    @Column(nullable = false)
    public String role = "STUDENT";

    @Column(nullable = false)
    public boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public int streak = 0;

    @Column(name = "last_active_date")
    public LocalDate lastActiveDate;

    public User() {
    }

    public User(String name, String email, String hash) {
        this.fullName = name;
        this.email = email;
        this.passwordHash = hash;
        this.role = "STUDENT";
        this.enabled = true;
        this.streak = 0;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void beforeSave() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (role == null || role.isBlank()) {
            role = "STUDENT";
        }
    }
}
