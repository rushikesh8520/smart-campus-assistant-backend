package edu.smartcampus.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.smartcampus.model.StudentChatMessage;
import edu.smartcampus.model.User;
import edu.smartcampus.repo.StudentChatMessageRepository;
import edu.smartcampus.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/student/chat")
public class StudentChatController {
    private final StudentChatMessageRepository chats;
    private final UserRepository users;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public StudentChatController(StudentChatMessageRepository chats,
                                 UserRepository users,
                                 ObjectMapper mapper,
                                 @Value("${app.openai.api-key:}") String apiKey,
                                 @Value("${app.openai.model:gpt-4o-mini}") String model) {
        this.chats = chats;
        this.users = users;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public record ChatRequest(String message) {}

    @GetMapping("/history")
    public List<StudentChatMessage> history(java.security.Principal principal) {
        User student = requireStudent(principal);
        return chats.findTop100ByStudentEmailOrderByCreatedAtAsc(student.email);
    }

    @PostMapping
    public StudentChatMessage send(@RequestBody ChatRequest request,
                                   java.security.Principal principal) {
        User student = requireStudent(principal);
        String message = request == null || request.message() == null ? "" : request.message().trim();
        if (message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a message.");
        }
        if (message.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be 4000 characters or fewer.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI API key is not configured. Set OPENAI_API_KEY in the backend environment.");
        }

        List<StudentChatMessage> prior = chats.findTop100ByStudentEmailOrderByCreatedAtAsc(student.email);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are Smart Campus Assistant, a friendly student study tutor. Explain CSE and general study topics clearly. " +
                "For campus-specific facts, only state information supplied by the user or clearly available in this conversation; " +
                "do not invent schedules, assignment deadlines, marks, attendance, or university policies. " +
                "If campus data is unavailable, explain how the student can check the relevant Smart Campus page. " +
                "Encourage learning and do not ask for passwords or sensitive personal data."));
        int start = Math.max(0, prior.size() - 12);
        for (int i = start; i < prior.size(); i++) {
            StudentChatMessage old = prior.get(i);
            messages.add(Map.of("role", "user", "content", old.userMessage == null ? "" : old.userMessage));
            messages.add(Map.of("role", "assistant", "content", old.assistantMessage == null ? "" : old.assistantMessage));
        }
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.5);
        body.put("max_tokens", 900);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode root = response.getBody();
            JsonNode textNode = root == null ? null : root.path("choices").path(0).path("message").path("content");
            String answer = textNode == null ? "" : textNode.asText("").trim();
            if (answer.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "OpenAI returned an empty answer. Please try again.");
            }
            StudentChatMessage saved = new StudentChatMessage();
            saved.studentEmail = student.email;
            saved.userMessage = message;
            saved.assistantMessage = answer;
            saved.createdAt = LocalDateTime.now();
            return chats.save(saved);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "OpenAI API request failed (HTTP " + ex.getStatusCode().value() + "). Check backend logs, API key, model, and API billing/limits.");
        }
    }

    private User requireStudent(java.security.Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        }
        User user = users.findByEmail(principal.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
        if (!"STUDENT".equalsIgnoreCase(user.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "AI chat is available to student accounts only.");
        }
        return user;
    }
}
