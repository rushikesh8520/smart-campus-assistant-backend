package edu.smartcampus.controller;

import edu.smartcampus.model.*;
import edu.smartcampus.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {
    private final ClassSessionRepository classes;
    private final UserRepository users;
    private final FacultyAssignmentRepository assignments;
    private final StudentMarkRepository marks;
    private final StudentAttendanceRepository attendance;

    public FacultyController(ClassSessionRepository classes, UserRepository users,
            FacultyAssignmentRepository assignments, StudentMarkRepository marks,
            StudentAttendanceRepository attendance) {
        this.classes = classes;
        this.users = users;
        this.assignments = assignments;
        this.marks = marks;
        this.attendance = attendance;
    }

    public record ScheduleRequest(String subject, String classDate, String startsAt,
            String endsAt, String roomName, String buildingName, String instructor) {}
    public record AssignmentRequest(String title, String course, String description,
            String dueDate, String studentId) {}
    public record MarkRequest(String studentId, String course, String examName,
            Double marksObtained, Double maxMarks) {}
    public record AttendanceRequest(String studentId, String course, String classDate,
            String status) {}

    @GetMapping("/schedule")
    public List<ClassSession> listSchedule() {
        return classes.findAll().stream()
                .sorted(Comparator.comparing((ClassSession c) -> c.classDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(c -> c.startsAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @PostMapping("/schedule")
    public ClassSession createSchedule(@RequestBody ScheduleRequest r) {
        validateSchedule(r);
        ClassSession c = new ClassSession();
        applySchedule(c, r);
        return classes.save(c);
    }

    @PutMapping("/schedule/{id}")
    public ClassSession updateSchedule(@PathVariable Long id, @RequestBody ScheduleRequest r) {
        validateSchedule(r);
        ClassSession c = classes.findById(id).orElseThrow(() -> notFound("Schedule entry not found"));
        applySchedule(c, r);
        return classes.save(c);
    }

    @DeleteMapping("/schedule/{id}")
    public Map<String, String> deleteSchedule(@PathVariable Long id) {
        ClassSession c = classes.findById(id).orElseThrow(() -> notFound("Schedule entry not found"));
        classes.delete(c);
        return Map.of("message", "Schedule deleted");
    }

    private void validateSchedule(ScheduleRequest r) {
        if (r == null || blank(r.subject()) || blank(r.classDate()) || blank(r.startsAt())
                || blank(r.endsAt()) || blank(r.roomName()) || blank(r.buildingName()))
            throw badRequest("Subject, date, times, room, and building are required");
        try {
            LocalTime start = LocalTime.parse(r.startsAt());
            LocalTime end = LocalTime.parse(r.endsAt());
            LocalDate.parse(r.classDate());
            if (!end.isAfter(start)) throw badRequest("End time must be after start time");
        } catch (java.time.format.DateTimeParseException ex) {
            throw badRequest("Use date YYYY-MM-DD and times HH:mm");
        }
    }

    private void applySchedule(ClassSession c, ScheduleRequest r) {
        c.subject = r.subject().trim();
        c.classDate = LocalDate.parse(r.classDate());
        c.startsAt = LocalTime.parse(r.startsAt());
        c.endsAt = LocalTime.parse(r.endsAt());
        c.roomName = r.roomName().trim();
        c.buildingName = r.buildingName().trim();
        c.instructor = r.instructor() == null ? "" : r.instructor().trim();
    }

    @GetMapping("/assignments")
    public List<Map<String, Object>> listAssignments() {
        return assignments.findAllByOrderByDueDateAsc().stream().map(this::assignmentView).toList();
    }

    @PostMapping("/assignments")
    public Map<String, Object> createAssignment(@RequestBody AssignmentRequest r) {
        if (r == null || blank(r.title()) || blank(r.course()) || blank(r.dueDate()))
            throw badRequest("Assignment title, course, and due date are required");
        FacultyAssignment a = new FacultyAssignment();
        a.title = r.title().trim();
        a.course = r.course().trim();
        a.description = r.description() == null ? "" : r.description().trim();
        try { a.dueDate = LocalDate.parse(r.dueDate()); }
        catch (Exception ex) { throw badRequest("Due date must use YYYY-MM-DD"); }
        a.studentId = parseOptionalId(r.studentId());
        if (a.studentId != null) requireStudent(a.studentId);
        a.status = "PUBLISHED";
        return assignmentView(assignments.save(a));
    }

    private Map<String, Object> assignmentView(FacultyAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id); m.put("title", a.title); m.put("course", a.course);
        m.put("description", a.description); m.put("dueDate", a.dueDate.toString());
        m.put("studentId", a.studentId == null ? "" : a.studentId.toString());
        m.put("studentName", a.studentId == null ? "All students" : studentName(a.studentId));
        m.put("status", a.status);
        return m;
    }

    @GetMapping("/marks")
    public List<Map<String, Object>> listMarks() {
        return marks.findAllByOrderByIdDesc().stream().map(this::markView).toList();
    }

    @PostMapping("/marks")
    public Map<String, Object> createMark(@RequestBody MarkRequest r) {
        if (r == null || blank(r.studentId()) || blank(r.course()) || blank(r.examName())
                || r.marksObtained() == null || r.maxMarks() == null)
            throw badRequest("Student, course, assessment, and both marks are required");
        Long studentId = parseRequiredId(r.studentId(), "studentId");
        requireStudent(studentId);
        if (r.maxMarks() <= 0 || r.marksObtained() < 0 || r.marksObtained() > r.maxMarks())
            throw badRequest("Marks obtained must be between 0 and maximum marks");
        StudentMark m = new StudentMark();
        m.studentId = studentId; m.course = r.course().trim(); m.examName = r.examName().trim();
        m.marksObtained = r.marksObtained(); m.maxMarks = r.maxMarks();
        return markView(marks.save(m));
    }

    private Map<String, Object> markView(StudentMark m) {
        return Map.of("id", m.id, "studentId", m.studentId, "studentName", studentName(m.studentId),
                "course", m.course, "examName", m.examName, "marksObtained", m.marksObtained,
                "maxMarks", m.maxMarks);
    }

    @GetMapping("/attendance")
    public List<Map<String, Object>> listAttendance() {
        return attendance.findAllByOrderByClassDateDesc().stream().map(this::attendanceView).toList();
    }

    @PostMapping("/attendance")
    public Map<String, Object> saveAttendance(@RequestBody AttendanceRequest r) {
        if (r == null || blank(r.studentId()) || blank(r.course()) || blank(r.classDate()) || blank(r.status()))
            throw badRequest("Student, course, date, and status are required");
        Long studentId = parseRequiredId(r.studentId(), "studentId");
        requireStudent(studentId);
        LocalDate date;
        try { date = LocalDate.parse(r.classDate()); }
        catch (Exception ex) { throw badRequest("Date must use YYYY-MM-DD"); }
        String status = r.status().trim().toUpperCase(Locale.ROOT);
        if (!List.of("PRESENT", "ABSENT", "LATE").contains(status))
            throw badRequest("Status must be PRESENT, ABSENT, or LATE");
        StudentAttendance a = attendance.findByStudentIdAndCourseIgnoreCaseAndClassDate(studentId, r.course().trim(), date)
                .orElseGet(StudentAttendance::new);
        a.studentId = studentId; a.course = r.course().trim(); a.classDate = date; a.status = status;
        return attendanceView(attendance.save(a));
    }

    private Map<String, Object> attendanceView(StudentAttendance a) {
        return Map.of("id", a.id, "studentId", a.studentId, "studentName", studentName(a.studentId),
                "course", a.course, "classDate", a.classDate.toString(), "status", a.status);
    }

    @GetMapping("/scoreboard")
    public List<Map<String, Object>> scoreboard() {
        List<User> studentUsers = users.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.role)).toList();
        List<StudentMark> allMarks = marks.findAll();
        List<StudentAttendance> allAttendance = attendance.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : studentUsers) {
            double obtained = 0, maximum = 0;
            for (StudentMark m : allMarks) if (m.studentId.equals(u.id)) {
                obtained += m.marksObtained; maximum += m.maxMarks;
            }
            List<StudentAttendance> records = allAttendance.stream().filter(a -> a.studentId.equals(u.id)).toList();
            long present = records.stream().filter(a -> "PRESENT".equals(a.status) || "LATE".equals(a.status)).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", u.id); row.put("name", u.fullName); row.put("email", u.email);
            row.put("score", maximum == 0 ? 0 : Math.round(obtained / maximum * 100.0));
            row.put("totalMarks", obtained); row.put("attendancePercentage", records.isEmpty() ? 0 : Math.round(present * 100.0 / records.size()));
            result.add(row);
        }
        result.sort((a, b) -> Double.compare(((Number)b.get("score")).doubleValue(), ((Number)a.get("score")).doubleValue()));
        return result;
    }

    private void requireStudent(Long id) {
        User u = users.findById(id).orElseThrow(() -> notFound("Student account not found"));
        if (!"STUDENT".equalsIgnoreCase(u.role)) throw badRequest("Selected account is not a student");
    }
    private String studentName(Long id) {
        return users.findById(id).map(u -> u.fullName).orElse("Deleted student");
    }
    private Long parseOptionalId(String value) {
        if (blank(value)) return null;
        return parseRequiredId(value, "studentId");
    }
    private Long parseRequiredId(String value, String field) {
        try { return Long.valueOf(value); }
        catch (Exception ex) { throw badRequest(field + " must be a valid numeric ID"); }
    }
    private boolean blank(String s) { return s == null || s.isBlank(); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
