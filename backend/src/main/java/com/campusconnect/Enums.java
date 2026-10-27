package com.campusconnect;
enum EventStatus { DRAFT, PUBLISHED, CANCELLED, COMPLETED }
enum RegStatus { CONFIRMED, WAITLISTED, CANCELLED }
class CampusException extends RuntimeException { final int status;
  CampusException(int status, String m){ super(m); this.status=status; } }
