package edu.smartcampus.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "student_attendance", uniqueConstraints = @UniqueConstraint(
    name = "uk_student_course_date", columnNames = {"student_id", "course", "class_date"}))
public class StudentAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "student_id", nullable = false)
    public Long studentId;

    @Column(nullable = false, length = 120)
    public String course;

    @Column(name = "class_date", nullable = false)
    public LocalDate classDate;

    @Column(nullable = false, length = 20)
    public String status;

    public StudentAttendance() {}
}
