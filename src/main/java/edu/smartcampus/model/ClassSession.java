package edu.smartcampus.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity @Table(name="class_sessions")
public class ClassSession {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String subject; public String roomName; public String buildingName;
 public LocalDate classDate; public LocalTime startsAt; public LocalTime endsAt;
 public String instructor;
 public ClassSession() {}
}
