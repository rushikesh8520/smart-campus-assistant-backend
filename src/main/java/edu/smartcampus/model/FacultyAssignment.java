package edu.smartcampus.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "faculty_assignments")
public class FacultyAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 180)
    public String title;

    @Column(nullable = false, length = 120)
    public String course;

    @Column(length = 2000)
    public String description;

    @Column(nullable = false)
    public LocalDate dueDate;

    public Long studentId;

    @Column(nullable = false, length = 30)
    public String status = "PUBLISHED";

    public FacultyAssignment() {}
}
