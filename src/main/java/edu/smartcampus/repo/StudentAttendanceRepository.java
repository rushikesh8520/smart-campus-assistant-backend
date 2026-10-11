package edu.smartcampus.repo;

import edu.smartcampus.model.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {
    List<StudentAttendance> findAllByOrderByClassDateDesc();
    List<StudentAttendance> findByStudentId(Long studentId);
    Optional<StudentAttendance> findByStudentIdAndCourseIgnoreCaseAndClassDate(Long studentId, String course, LocalDate classDate);
}
