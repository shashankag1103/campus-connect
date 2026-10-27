# CampusConnect (core build)
## Run backend (Java 17+, Maven)
cd backend
# Option A - MySQL (create nothing; DB is auto-created). Set DB_USER / DB_PASS if not root/root:
mvn spring-boot:run
# Option B - no MySQL, embedded H2 file DB:
#   Windows PowerShell: $env:DB_URL="jdbc:h2:file:./data/cc"; $env:DB_USER="sa"; $env:DB_PASS=""; mvn spring-boot:run
## Run frontend (Node 18+)
cd frontend && npm install && npm run dev   # http://localhost:5173
## Try it
Open the app, register Aisha for "React Workshop" (capacity 2), then Rahul, then switch users to see waitlist; cancel to see promotion.
CSV export: http://localhost:8080/api/events/2/export.csv
Notifications: GET /api/users/1/notifications
## OOP map
Encapsulation: Event (assertOpenForRegistration, validated constructor), BaseUser setters
Abstraction: RegistrationService, NotificationService interfaces
Inheritance: BaseUser -> Student, Organizer (JOINED mapping, shared columns in base_user)
Polymorphism: InAppNotificationService + EmailNotificationService injected as List<NotificationService>; getRole()/canManage() overrides
Concurrency: EventRepository.lockById (PESSIMISTIC_WRITE) + unique(event_id, student_id)
