package edu.smartcampus.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student_marks")
public class StudentMark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Long studentId;

    @Column(nullable = false, length = 120)
    public String course;

    @Column(nullable = false, length = 180)
    public String examName;

    @Column(nullable = false)
    public double marksObtained;

    @Column(nullable = false)
    public double maxMarks;

    public StudentMark() {}
}
