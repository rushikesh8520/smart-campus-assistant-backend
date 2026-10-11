package edu.smartcampus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_chat_messages", indexes = {
        @Index(name = "idx_student_chat_email_created", columnList = "student_email, created_at")
})
public class StudentChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "student_email", nullable = false, length = 190)
    public String studentEmail;

    @Column(name = "user_message", nullable = false, length = 4000)
    public String userMessage;

    @Column(name = "assistant_message", nullable = false, columnDefinition = "TEXT")
    public String assistantMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    public StudentChatMessage() {}

    @PrePersist
    public void beforeInsert() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
