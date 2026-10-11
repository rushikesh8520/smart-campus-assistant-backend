package edu.smartcampus;

import edu.smartcampus.model.User;
import edu.smartcampus.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminPasswordReset {

    @Bean
    CommandLineRunner resetAdminPassword(
            UserRepository users,
            PasswordEncoder encoder) {

        return args -> {
            String email = "admin@smartcampus.local";
            String password = "CampusAdmin@2026";

            User admin = users.findByEmail(email)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Admin account not found: " + email));

            admin.role = "ADMIN";
            admin.enabled = true;
            admin.passwordHash = encoder.encode(password);

            users.save(admin);

            System.out.println("Admin password reset successfully.");
        };
    }
}