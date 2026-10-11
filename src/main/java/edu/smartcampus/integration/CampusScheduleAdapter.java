package edu.smartcampus.integration;

/**
 * Integration boundary for the institution's authorized SIS/LMS/calendar API.
 * Implement this adapter after receiving official API docs and credentials.
 * Normalize external data into ClassSession/assignment records; never hard-code
 * student credentials or scrape authenticated portals.
 */
public interface CampusScheduleAdapter {
  // Example future contract: List<ClassSession> fetchSchedule(String studentId, LocalDate from, LocalDate to);
}
