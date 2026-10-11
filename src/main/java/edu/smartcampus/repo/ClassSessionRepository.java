package edu.smartcampus.repo;
import edu.smartcampus.model.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface ClassSessionRepository extends JpaRepository<ClassSession,Long> {
 List<ClassSession> findByClassDateOrderByStartsAt(LocalDate date);
}
