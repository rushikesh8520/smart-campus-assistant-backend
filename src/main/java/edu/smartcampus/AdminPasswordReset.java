package edu.smartcampus;

import edu.smartcampus.model.User;
import edu.smartcampus.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminPasswordReset {

    @Bean
    CommandLineRunner resetAdminPassword(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${ADMIN_RESET_PASSWORD_ON_STARTUP:false}")
            boolean resetEnabled,
            @Value("${ADMIN_EMAIL:admin@smartcampus.local}")
            String email,
            @Value("${ADMIN_PASSWORD:}")
            String password) {

        return args -> {
            if (!resetEnabled) {
                System.out.println(
                    "Admin password reset is disabled."
                );
                return;
            }

            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                    "ADMIN_PASSWORD must be configured."
                );
            }

            String normalizedEmail = email.trim().toLowerCase();

            User admin = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException(
                    "Admin account not found: " + normalizedEmail
                ));

            admin.role = "ADMIN";
            admin.enabled = true;
            admin.passwordHash = encoder.encode(password);

            users.save(admin);

            System.out.println(
                "Admin password reset successfully."
            );
        };
    }
}