package com.campusconnect;
import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/** Inheritance: JOINED mapping. Shared columns in base_user, specialised columns in subclass tables. */
@Entity @Inheritance(strategy = InheritanceType.JOINED)
abstract class BaseUser {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String name;
  @Column(nullable=false, unique=true) private String email;
  protected BaseUser(){}
  protected BaseUser(String name, String email){ setName(name); setEmail(email); }
  public Long getId(){ return id; } public String getName(){ return name; } public String getEmail(){ return email; }
  public void setName(String n){ if(n==null||n.isBlank()) throw new CampusException(400,"Name required"); name=n.trim(); }
  public void setEmail(String e){ if(e==null||!e.contains("@")) throw new CampusException(400,"Valid email required"); email=e.toLowerCase(); }
  @Column(length=100) private String passwordHash;
  private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);
  public void setPassword(String raw) {
    if (raw == null || raw.length() < 8) throw new CampusException(400, "Password must be at least 8 characters");
    passwordHash = PASSWORD_ENCODER.encode(raw);
  }
  public boolean checkPassword(String raw) {
    return raw != null && passwordHash != null && PASSWORD_ENCODER.matches(raw, passwordHash);
  }
  public boolean hasPassword() { return passwordHash != null && !passwordHash.isBlank(); }
  public String getPasswordHash(){ return passwordHash; } public void setPasswordHash(String h){ passwordHash=h; }
  public abstract String getRole();           // polymorphic
  public abstract boolean canManage(Event e); // polymorphic
}
@Entity class Student extends BaseUser { private String studentNumber;
  protected Student(){} Student(String n,String e,String sn){ super(n,e); studentNumber=sn; }
  public String getStudentNumber(){ return studentNumber; }
  @Override public String getRole(){ return "STUDENT"; }
  @Override public boolean canManage(Event e){ return false; } }
@Entity class Organizer extends BaseUser { private String club;
  protected Organizer(){} Organizer(String n,String e,String c){ super(n,e); club=c; }
  public String getClub(){ return club; }
  @Override public String getRole(){ return "ORGANIZER"; }
  @Override public boolean canManage(Event e){ return e.getOrganizer().getId().equals(getId()); } }
