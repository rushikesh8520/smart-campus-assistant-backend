package edu.smartcampus.repo;

import edu.smartcampus.model.FacultyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacultyAssignmentRepository extends JpaRepository<FacultyAssignment, Long> {
    List<FacultyAssignment> findAllByOrderByDueDateAsc();
}
