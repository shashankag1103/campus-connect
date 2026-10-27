package com.campusconnect;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(indexes=@Index(columnList="startTime"))
class Event {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private String title, category, venue, imageUrl; @Column(length=2000) private String description;
  private LocalDateTime startTime, registrationDeadline; private int capacity;
  @Enumerated(EnumType.STRING) private EventStatus status = EventStatus.PUBLISHED;
  @ManyToOne(optional=false) private Organizer organizer;
  private double price; private LocalDateTime endTime; private boolean featured;
  @Column(length=1000) private String agenda, tags, eligibility;
  protected Event(){}
  Event enrich(double price,LocalDateTime end,boolean featured,String agenda,String tags,String eligibility){
    if(price<0) throw new CampusException(400,"Price cannot be negative");
    if(end.isBefore(startTime)) throw new CampusException(400,"End must be after start");
    this.price=price;endTime=end;this.featured=featured;this.agenda=agenda;this.tags=tags;this.eligibility=eligibility; return this; }
  Event(String title,String cat,String venue,String desc,String img,LocalDateTime start,LocalDateTime deadline,int cap,Organizer o){
    if(cap<1) throw new CampusException(400,"Capacity must be positive");
    if(deadline.isAfter(start)) throw new CampusException(400,"Deadline must be before start");
    this.title=title;category=cat;this.venue=venue;description=desc;imageUrl=img;startTime=start;registrationDeadline=deadline;capacity=cap;organizer=o; }
  /** Business rule kept inside the entity (encapsulation). */
  void assertOpenForRegistration(LocalDateTime now){
    if(status!=EventStatus.PUBLISHED) throw new CampusException(409,"Event is "+status.name().toLowerCase());
    if(now.isAfter(registrationDeadline)) throw new CampusException(409,"Registration deadline has passed"); }
  void cancel(){ status=EventStatus.CANCELLED; }
  public Long getId(){return id;} public String getTitle(){return title;} public String getCategory(){return category;}
  public String getVenue(){return venue;} public String getDescription(){return description;} public String getImageUrl(){return imageUrl;}
  public LocalDateTime getStartTime(){return startTime;} public LocalDateTime getRegistrationDeadline(){return registrationDeadline;}
  public int getCapacity(){return capacity;} public EventStatus getStatus(){return status;} public Organizer getOrganizer(){return organizer;}
  public double getPrice(){return price;} public LocalDateTime getEndTime(){return endTime;} public boolean isFeatured(){return featured;}
  public String getAgenda(){return agenda;} public String getTags(){return tags;} public String getEligibility(){return eligibility;}
}
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"event_id","student_id"}))
class Registration {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional=false) private Event event; @ManyToOne(optional=false) private Student student;
  @Enumerated(EnumType.STRING) private RegStatus status; private LocalDateTime registeredAt = LocalDateTime.now();
  protected Registration(){} Registration(Event e, Student s, RegStatus st){ event=e; student=s; status=st; }
  void promote(){ if(status!=RegStatus.WAITLISTED) throw new CampusException(409,"Not waitlisted"); status=RegStatus.CONFIRMED; }
  void cancel(){ status=RegStatus.CANCELLED; }
  void setStatusForRegister(RegStatus s){ status=s; registeredAt=LocalDateTime.now(); }
  public Long getId(){return id;} public Event getEvent(){return event;} public Student getStudent(){return student;}
  public RegStatus getStatus(){return status;} public LocalDateTime getRegisteredAt(){return registeredAt;}
}
@Entity class Notification { @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long userId; private String message; private LocalDateTime createdAt = LocalDateTime.now();
  protected Notification(){} Notification(Long u,String m){ userId=u; message=m; }
  public Long getId(){return id;} public Long getUserId(){return userId;} public String getMessage(){return message;} public LocalDateTime getCreatedAt(){return createdAt;} }
