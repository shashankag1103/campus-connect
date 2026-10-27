package com.campusconnect;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/**
 * Stores hackathon-specific details for a registration.
 * One-to-one with Registration; only exists for Hackathon-category events.
 * teammates is stored as a JSON string to avoid a separate table while staying
 * within a single-file project structure.
 */
@Entity
class HackathonDetail {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @OneToOne(optional=false)
  @JoinColumn(name="registration_id", unique=true)
  private Registration registration;
  private String teamName;
  private int teamSize;
  /** JSON array: [{name, email, studentId}, ...] */
  @Column(length=4000)
  private String teammates;
  @Column(length=2000)
  private String skills;
  @Column(length=3000)
  private String projectIdea;
  @Column(length=1000)
  private String accessibilityNote;
  private LocalDateTime submittedAt = LocalDateTime.now();
  protected HackathonDetail(){}
  HackathonDetail(Registration reg, String teamName, int teamSize,
                  String teammates, String skills, String projectIdea, String accessibilityNote){
    this.registration=reg; this.teamName=teamName; this.teamSize=teamSize;
    this.teammates=teammates; this.skills=skills;
    this.projectIdea=projectIdea; this.accessibilityNote=accessibilityNote;
  }
  void updateDetails(String teamName, int teamSize, String teammates, String skills,
                     String projectIdea, String accessibilityNote) {
    this.teamName=teamName; this.teamSize=teamSize; this.teammates=teammates;
    this.skills=skills; this.projectIdea=projectIdea; this.accessibilityNote=accessibilityNote;
  }
  public Long getId(){ return id; }
  public Registration getRegistration(){ return registration; }
  public String getTeamName(){ return teamName; }
  public int getTeamSize(){ return teamSize; }
  public String getTeammates(){ return teammates; }
  public String getSkills(){ return skills; }
  public String getProjectIdea(){ return projectIdea; }
  public String getAccessibilityNote(){ return accessibilityNote; }
  public LocalDateTime getSubmittedAt(){ return submittedAt; }
}
