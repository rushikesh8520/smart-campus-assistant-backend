
package edu.smartcampus.controller;

import edu.smartcampus.model.User;
import edu.smartcampus.repo.UserRepository;
import edu.smartcampus.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(
            UserRepository users,
            PasswordEncoder encoder,
            JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank String password) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {
    }

    @PostMapping("/register")
    public ResponseEntity<?> register() {
        return ResponseEntity.status(403).body(
                Map.of(
                        "message",
                        "Registration is disabled. Ask your campus administrator to create your account."
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        var result = users.findByEmail(email);

        if (result.isEmpty()) {
            return ResponseEntity.status(401).body(
                    Map.of("message", "Invalid email or password")
            );
        }

        User user = result.get();

        if (!user.enabled ||
                !encoder.matches(
                        request.password(),
                        user.passwordHash)) {
            return ResponseEntity.status(401).body(
                    Map.of("message", "Invalid email or password")
            );
        }

        String role = user.role == null
                ? "STUDENT"
                : user.role.trim().toUpperCase();

        String token = jwt.generate(user.email, role);

        return ResponseEntity.ok(
                Map.of(
                        "token", token,
                        "name", user.fullName,
                        "email", user.email,
                        "role", role
                )
        );
    }
}
