package com.campusconnect;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@SpringBootApplication
public class CampusConnectApplication {
  public static void main(String[] args) {
    SpringApplication.run(CampusConnectApplication.class, args);
  }

  @Bean
  WebMvcConfigurer cors(@Value("$" + "{cors.origin}") String origin) {
    return new WebMvcConfigurer() {
      @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origin)
            .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type");
      }
    };
  }

  private static final String[] IMAGES = {
      "photo-1504384308090-c894fdcc538d", "photo-1517694712202-14dd9538aa97",
      "photo-1492684223066-81342ee5ff30", "photo-1517649763962-0c623066013b",
      "photo-1540575467063-178a50c2df87", "photo-1531482615713-2afd69097998",
      "photo-1523580846011-d3a5bc25702b", "photo-1511795409834-ef04bbd61622"
  };

  private static Event sample(Organizer organizer, String title, String category, String venue,
      String description, int days, int hour, int capacity, double price,
      boolean featured, String tags, int image) {
    LocalDateTime start = LocalDateTime.now().plusDays(days)
        .withHour(hour).withMinute(0).withSecond(0).withNano(0);
    return new Event(title, category, venue, description,
        "https://images.unsplash.com/" + IMAGES[image % IMAGES.length] + "?w=1200",
        start, start.minusDays(1), capacity, organizer)
        .enrich(price, start.plusHours(3), featured,
            "Welcome and check-in|Main session|Q&A and wrap-up", tags, "All enrolled students");
  }

  @Bean
  CommandLineRunner seed(EventRepository events, StudentRepository students,
                         OrganizerRepository organizers) {
    return args -> {
      seedStudent(students, "Aisha Khan", "aisha@campus.edu", "S1001");
      seedStudent(students, "Rahul Verma", "rahul@campus.edu", "S1002");
      seedStudent(students, "Meera Nair", "meera@campus.edu", "S1003");
      seedStudent(students, "Daniel Lee", "daniel@campus.edu", "S1004");
      seedOrganizer(organizers, "Coding Club", "coding@campus.edu", "Coding Club");
      seedOrganizer(organizers, "Sports Council", "sports@campus.edu", "Sports Council");
      seedOrganizer(organizers, "Student Affairs", "affairs@campus.edu", "Student Affairs");

      Organizer coding = organizers.findByEmail("coding@campus.edu").orElseThrow();
      Organizer sports = organizers.findByEmail("sports@campus.edu").orElseThrow();
      Organizer affairs = organizers.findByEmail("affairs@campus.edu").orElseThrow();
      Set<String> titles = new HashSet<>();
      events.findAll().forEach(e -> titles.add(e.getTitle()));

      List<Event> samples = List.of(
          sample(coding, "React Workshop", "Workshop", "Innovation Lab",
              "Build a small React app with the Coding Club. Beginners are welcome.",
              5, 14, 2, 0, true, "react,frontend,beginner", 0),
          sample(coding, "Competitive Coding Contest", "Hackathon", "Computer Lab 1",
              "Solve algorithmic problems with a team and present a working prototype.",
              15, 13, 60, 0, true, "algorithms,contest,team", 1),
          sample(affairs, "AI in Healthcare Seminar", "Seminar", "Lecture Hall B",
              "Campus researchers discuss how machine learning is changing healthcare.",
              7, 11, 150, 0, true, "ai,health,talk", 2),
          sample(affairs, "Campus Cultural Night", "Cultural", "Main Quad",
              "An evening of student performances, food, and campus traditions.",
              16, 18, 500, 5, true, "music,dance,food", 3),
          sample(sports, "Inter-Department Football Cup", "Sports", "University Stadium",
              "A campus tournament open to all skill levels. Register your squad or cheer.",
              12, 10, 200, 2, false, "football,tournament", 4),
          sample(coding, "Cybersecurity Capture the Flag", "Hackathon", "Computer Lab 2",
              "A team challenge with web, cryptography, and forensics puzzles.",
              18, 10, 80, 0, true, "security,ctf,teams", 5),
          sample(affairs, "Green Campus Volunteer Day", "Social Impact", "Student Union",
              "Join an afternoon of practical campus sustainability projects.",
              20, 9, 100, 0, false, "volunteer,sustainability", 6)
      );
      samples.stream().filter(e -> !titles.contains(e.getTitle())).forEach(events::save);
    };
  }

  private static void seedStudent(StudentRepository repo, String name, String email, String number) {
    Student student = repo.findByEmail(email)
        .orElseGet(() -> repo.save(new Student(name, email, number)));
    if (!student.hasPassword()) {
      student.setPassword("Campus@123");
      repo.save(student);
    }
  }

  private static void seedOrganizer(OrganizerRepository repo, String name, String email, String club) {
    Organizer organizer = repo.findByEmail(email)
        .orElseGet(() -> repo.save(new Organizer(name, email, club)));
    if (!organizer.hasPassword()) {
      organizer.setPassword("Organizer@123");
      repo.save(organizer);
    }
  }
}

record EventView(Long id, String title, String category, String venue, String description,
    String imageUrl, LocalDateTime startTime, LocalDateTime endTime,
    LocalDateTime registrationDeadline, int capacity, long registered, long seatsLeft,
    String status, String organizer, String contact, double price, boolean featured,
    String agenda, String tags, String eligibility) {}
record HackathonDetailView(Long id, String teamName, int teamSize, String teammates,
    String skills, String projectIdea, String accessibilityNote, LocalDateTime submittedAt) {}
record AttendeeView(Long registrationId, String studentName, String studentEmail,
    String studentNumber, String status, LocalDateTime registeredAt,
    HackathonDetailView hackathonDetail) {}
record HackathonRegRequest(String teamName, int teamSize, String teammates,
    String skills, String projectIdea, String accessibilityNote) {}

@RestController
@RequestMapping("/api")
class ApiController {
  private final EventRepository events;
  private final RegistrationRepository registrations;
  private final RegistrationService service;
  private final NotificationRepository notifications;
  private final HackathonDetailRepository hackDetails;

  ApiController(EventRepository events, RegistrationRepository registrations,
      RegistrationService service, NotificationRepository notifications,
      HackathonDetailRepository hackDetails) {
    this.events = events;
    this.registrations = registrations;
    this.service = service;
    this.notifications = notifications;
    this.hackDetails = hackDetails;
  }

  private Student requireStudent(HttpServletRequest request) {
    Object current = request.getAttribute(AuthInterceptor.ATTR);
    if (current instanceof Student student) return student;
    throw new CampusException(401, "Sign in as a student to continue");
  }

  private EventView view(Event e) {
    long count = registrations.countByEventIdAndStatus(e.getId(), RegStatus.CONFIRMED);
    return new EventView(e.getId(), e.getTitle(), e.getCategory(), e.getVenue(),
        e.getDescription(), e.getImageUrl(), e.getStartTime(), e.getEndTime(),
        e.getRegistrationDeadline(), e.getCapacity(), count, Math.max(0, e.getCapacity()-count),
        e.getStatus().name(), e.getOrganizer().getName(), e.getOrganizer().getEmail(),
        e.getPrice(), e.isFeatured(), e.getAgenda(), e.getTags(), e.getEligibility());
  }

  private HackathonDetailView hackView(HackathonDetail h) {
    return h == null ? null : new HackathonDetailView(h.getId(), h.getTeamName(),
        h.getTeamSize(), h.getTeammates(), h.getSkills(), h.getProjectIdea(),
        h.getAccessibilityNote(), h.getSubmittedAt());
  }

  @GetMapping("/events")
  Map<String,Object> list(@RequestParam(required=false) String q,
      @RequestParam(required=false) String category,
      @RequestParam(required=false) Boolean featured,
      @RequestParam(defaultValue="soon") String sort,
      @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="9") int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new CampusException(400, "Invalid page or page size");
    Sort order = "newest".equalsIgnoreCase(sort)
        ? Sort.by(Sort.Direction.DESC, "id") : Sort.by("startTime");
    Page<Event> results = events.search(q == null || q.isBlank() ? null : q.trim(),
        category == null || category.isBlank() ? null : category.trim(),
        featured, PageRequest.of(page, size, order));
    return Map.of("items", results.map(this::view).getContent(),
        "page", page, "totalPages", results.getTotalPages(),
        "total", results.getTotalElements());
  }

  @GetMapping("/events/{id}")
  EventView one(@PathVariable Long id) {
    return view(events.findById(id)
        .orElseThrow(() -> new CampusException(404, "Event not found")));
  }

  @PostMapping("/events/{id}/registrations")
  Map<String,Object> register(@PathVariable Long id,
      @RequestParam(defaultValue="false") boolean waitlist, HttpServletRequest request) {
    Registration reg = service.register(id, requireStudent(request).getId(), waitlist);
    return Map.of("status", reg.getStatus().name());
  }

  @PostMapping("/events/{id}/registrations/hackathon")
  Map<String,Object> hackathonRegister(@PathVariable Long id,
      @RequestBody HackathonRegRequest body, HttpServletRequest request) {
    Student student = requireStudent(request);
    if (body == null || body.teamName() == null || body.teamName().isBlank())
      throw new CampusException(400, "Team name is required");
    if (body.teamSize() < 1 || body.teamSize() > 10)
      throw new CampusException(400, "Team size must be between 1 and 10");
    if (body.projectIdea() == null || body.projectIdea().isBlank())
      throw new CampusException(400, "Project idea is required");
    Event event = events.findById(id)
        .orElseThrow(() -> new CampusException(404, "Event not found"));
    if (!"Hackathon".equalsIgnoreCase(event.getCategory()))
      throw new CampusException(400, "This event is not a hackathon");
    if (body.teamName().length() > 120 || body.projectIdea().length() > 3000
        || (body.teammates() != null && body.teammates().length() > 4000)
        || (body.skills() != null && body.skills().length() > 2000)
        || (body.accessibilityNote() != null && body.accessibilityNote().length() > 1000))
      throw new CampusException(400, "One or more registration fields are too long");

    registrations.findByEventIdAndStudentId(id, student.getId()).ifPresent(existing -> {
      if (existing.getStatus() != RegStatus.CANCELLED)
        throw new CampusException(409, "Already registered for this hackathon");
    });
    Registration reg = service.register(id, student.getId(), true);
    HackathonDetail detail = hackDetails.findByRegistrationId(reg.getId())
        .orElseGet(() -> new HackathonDetail(reg, body.teamName().trim(),
            body.teamSize(), body.teammates(), body.skills(),
            body.projectIdea().trim(), body.accessibilityNote()));
    detail.updateDetails(body.teamName().trim(), body.teamSize(), body.teammates(),
        body.skills(), body.projectIdea().trim(), body.accessibilityNote());
    hackDetails.save(detail);
    return Map.of("status", reg.getStatus().name());
  }

  @DeleteMapping("/events/{id}/registrations/me")
  ResponseEntity<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
    service.cancel(id, requireStudent(request).getId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/users/{id}/registrations")
  List<Map<String,Object>> mine(@PathVariable Long id, HttpServletRequest request) {
    Student student = requireStudent(request);
    if (!student.getId().equals(id)) throw new CampusException(403, "Forbidden");
    return registrations.findByStudentIdOrderByRegisteredAtDesc(id).stream().map(reg -> {
      Map<String,Object> row = new LinkedHashMap<>();
      row.put("eventId", reg.getEvent().getId());
      row.put("title", reg.getEvent().getTitle());
      row.put("category", reg.getEvent().getCategory());
      row.put("status", reg.getStatus().name());
      row.put("registeredAt", reg.getRegisteredAt().toString());
      hackDetails.findByRegistrationId(reg.getId())
          .ifPresent(h -> row.put("hackathonDetail", hackView(h)));
      return row;
    }).toList();
  }

  @GetMapping("/users/{id}/notifications")
  List<Notification> myNotifications(@PathVariable Long id, HttpServletRequest request) {
    Object current = request.getAttribute(AuthInterceptor.ATTR);
    if (!(current instanceof BaseUser user))
      throw new CampusException(401, "Authentication required");
    if (!user.getId().equals(id)) throw new CampusException(403, "Forbidden");
    return notifications.findByUserIdOrderByCreatedAtDesc(id);
  }

  @GetMapping("/events/{id}/attendees")
  List<AttendeeView> attendees(@PathVariable Long id, HttpServletRequest request) {
    Object current = request.getAttribute(AuthInterceptor.ATTR);
    if (!(current instanceof BaseUser user))
      throw new CampusException(401, "Authentication required");
    Event event = events.findById(id)
        .orElseThrow(() -> new CampusException(404, "Event not found"));
    if (!user.canManage(event))
      throw new CampusException(403, "Only the event organizer can view attendees");
    return registrations.findByEventId(id).stream().map(reg -> {
      HackathonDetail detail = hackDetails.findByRegistrationId(reg.getId()).orElse(null);
      return new AttendeeView(reg.getId(), reg.getStudent().getName(),
          reg.getStudent().getEmail(), reg.getStudent().getStudentNumber(),
          reg.getStatus().name(), reg.getRegisteredAt(), hackView(detail));
    }).toList();
  }

  @GetMapping("/events/{id}/export.csv")
  ResponseEntity<byte[]> exportCsv(@PathVariable Long id, HttpServletRequest request)
      throws IOException {
    Object current = request.getAttribute(AuthInterceptor.ATTR);
    if (!(current instanceof BaseUser user))
      throw new CampusException(401, "Authentication required");
    Event event = events.findById(id)
        .orElseThrow(() -> new CampusException(404, "Event not found"));
    if (!user.canManage(event))
      throw new CampusException(403, "Only the event organizer can export attendees");
    byte[] csv = Files.readAllBytes(service.exportCsv(id));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=participants-" + id + ".csv")
        .contentType(MediaType.parseMediaType("text/csv")).body(csv);
  }
}

@RestControllerAdvice
class Errors {
  @ExceptionHandler(CampusException.class)
  ResponseEntity<Map<String,String>> campus(CampusException e) {
    return ResponseEntity.status(e.status).body(Map.of("error", e.getMessage()));
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String,String>> unexpected(Exception e) {
    return ResponseEntity.status(500).body(Map.of("error", "Unexpected server error"));
  }
}

