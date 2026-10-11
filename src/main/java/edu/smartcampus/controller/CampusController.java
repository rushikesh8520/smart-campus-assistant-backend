
package edu.smartcampus.controller;

import edu.smartcampus.model.*;
import edu.smartcampus.repo.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CampusController {

    private final ClassSessionRepository classes;
    private final CampusEventRepository events;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public CampusController(
            ClassSessionRepository classes,
            CampusEventRepository events,
            UserRepository users,
            PasswordEncoder passwordEncoder) {
        this.classes = classes;
        this.events = events;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    // Schedule and events

    @GetMapping("/schedule/today")
    public List<ClassSession> today() {
        return classes.findByClassDateOrderByStartsAt(LocalDate.now());
    }

    @GetMapping("/events")
    public List<CampusEvent> events() {
        return events.findByStartsAtAfterOrderByStartsAt(
                LocalDateTime.now());
    }

    @GetMapping("/assignments")
    public List<Map<String, Object>> assignments() {
        return List.of(
            Map.of(
                "id", 1,
                "title", "Review Data Structures notes",
                "course", "Data Structures",
                "dueDate", LocalDate.now().plusDays(2).toString(),
                "status", "IN_PROGRESS"
            ),
            Map.of(
                "id", 2,
                "title", "Submit DBMS lab record",
                "course", "DBMS",
                "dueDate", LocalDate.now().plusDays(4).toString(),
                "status", "TODO"
            )
        );
    }

    // Daily streak

    @PostMapping("/streak/check-in")
    public Map<String, Object> checkIn(java.security.Principal principal) {
        User user = currentUser(principal);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        if (!today.equals(user.lastActiveDate)) {
            if (today.minusDays(1).equals(user.lastActiveDate)) {
                user.streak++;
            } else {
                user.streak = 1;
            }

            user.lastActiveDate = today;
            users.save(user);
        }

        return Map.of(
            "streak", user.streak,
            "lastActiveDate", user.lastActiveDate.toString()
        );
    }

    @GetMapping("/streak")
    public Map<String, Object> streak(java.security.Principal principal) {
        User user = currentUser(principal);

        return Map.of(
            "streak", user.streak,
            "lastActiveDate",
            user.lastActiveDate == null
                ? ""
                : user.lastActiveDate.toString()
        );
    }

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> leaderboard() {
        return users.findAll().stream()
            .filter(user -> "STUDENT".equalsIgnoreCase(user.role))
            .sorted((a, b) -> Integer.compare(b.streak, a.streak))
            .limit(10)
            .map(user -> Map.<String, Object>of(
                "name", user.fullName,
                "streak", user.streak
            ))
            .toList();
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(
            java.security.Principal principal) {
        User user = currentUser(principal);

        return Map.of(
            "name", user.fullName,
            "email", user.email,
            "streak", user.streak,
            "role", user.role
        );
    }

    // Admin account management

    public record CreateAccountRequest(
        String fullName,
        String email,
        String password,
        String role
    ) {}

    @GetMapping("/admin/users")
    public List<Map<String, Object>> listUsers() {
        return users.findAll().stream()
            .map(user -> {
                Map<String, Object> account = new HashMap<>();
                account.put("id", user.id);
                account.put("name", user.fullName);
                account.put("email", user.email);
                account.put("role", user.role);
                account.put("streak", user.streak);
                account.put("enabled", user.enabled);
                return account;
            })
            .toList();
    }

    @PostMapping("/admin/users")
    public Map<String, Object> createUser(
            @RequestBody CreateAccountRequest request) {

        if (request.fullName() == null
                || request.fullName().isBlank()
                || request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().length() < 8) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Enter a name, email, and password of at least 8 characters"
            );
        }

        String email = request.email().trim().toLowerCase();
        String role = request.role() == null
            ? "STUDENT"
            : request.role().trim().toUpperCase();

        if (!List.of("STUDENT", "FACULTY").contains(role)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only STUDENT or FACULTY accounts can be created"
            );
        }

        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email already exists"
            );
        }

        User user = new User(
            request.fullName().trim(),
            email,
            passwordEncoder.encode(request.password())
        );

        user.role = role;
        user.enabled = true;

        User saved = users.save(user);

        return Map.of(
            "id", saved.id,
            "name", saved.fullName,
            "email", saved.email,
            "role", saved.role
        );
    }

    @DeleteMapping("/admin/users/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        User user = users.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found"
            ));

        if ("ADMIN".equalsIgnoreCase(user.role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Administrator accounts cannot be deleted here"
            );
        }

        users.delete(user);
        return Map.of("message", "Account deleted successfully");
    }

    // Faculty access to student accounts

    @GetMapping("/faculty/students")
    public List<Map<String, Object>> listStudents() {
        return users.findAll().stream()
            .filter(user -> "STUDENT".equalsIgnoreCase(user.role))
            .map(user -> {
                Map<String, Object> student = new HashMap<>();
                student.put("id", user.id);
                student.put("name", user.fullName);
                student.put("email", user.email);
                student.put("streak", user.streak);
                return student;
            })
            .toList();
    }

    private User currentUser(java.security.Principal principal) {
        return users.findByEmail(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found"
            ));
    }
}
