package edu.smartcampus.repo;
import edu.smartcampus.model.CampusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface CampusEventRepository extends JpaRepository<CampusEvent,Long> {
 List<CampusEvent> findByStartsAtAfterOrderByStartsAt(LocalDateTime now);
}
