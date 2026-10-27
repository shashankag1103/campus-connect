package com.campusconnect;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EventRepository extends JpaRepository<Event, Long> {
  @Query("select e from Event e where e.status = com.campusconnect.EventStatus.PUBLISHED and (:q is null or lower(e.title) like lower(concat('%',:q,'%')) or lower(e.venue) like lower(concat('%',:q,'%'))) and (:cat is null or e.category=:cat) and (:feat is null or e.featured=:feat)")
  Page<Event> search(@Param("q") String q, @Param("cat") String category,
                     @Param("feat") Boolean featured, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Event e where e.id=:id")
  Optional<Event> lockById(@Param("id") Long id);
}

interface UserRepository extends JpaRepository<BaseUser, Long> {
  Optional<BaseUser> findByEmail(String email);
}

interface StudentRepository extends JpaRepository<Student, Long> {
  Optional<Student> findByEmail(String email);
}

interface OrganizerRepository extends JpaRepository<Organizer, Long> {
  Optional<Organizer> findByEmail(String email);
}

interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}

interface RegistrationRepository extends JpaRepository<Registration, Long> {
  Optional<Registration> findByEventIdAndStudentId(Long eventId, Long studentId);
  long countByEventIdAndStatus(Long eventId, RegStatus status);
  List<Registration> findByEventIdAndStatusOrderByRegisteredAtAsc(Long eventId, RegStatus status);
  List<Registration> findByStudentIdOrderByRegisteredAtDesc(Long studentId);
  List<Registration> findByEventId(Long eventId);
}

interface HackathonDetailRepository extends JpaRepository<HackathonDetail, Long> {
  Optional<HackathonDetail> findByRegistrationId(Long registrationId);
}
