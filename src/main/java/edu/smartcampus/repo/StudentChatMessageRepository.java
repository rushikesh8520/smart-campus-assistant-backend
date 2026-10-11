package edu.smartcampus.repo;

import edu.smartcampus.model.StudentChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentChatMessageRepository extends JpaRepository<StudentChatMessage, Long> {
    List<StudentChatMessage> findTop100ByStudentEmailOrderByCreatedAtAsc(String studentEmail);
}
