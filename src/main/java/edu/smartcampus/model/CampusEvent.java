package edu.smartcampus.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="campus_events")
public class CampusEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String title; @Column(length=1200) public String description;
 public String location; public LocalDateTime startsAt;
 public CampusEvent() {}
}
