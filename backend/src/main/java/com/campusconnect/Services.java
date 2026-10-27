package com.campusconnect;
import java.io.*; import java.nio.file.*; import java.time.LocalDateTime; import java.util.*;
import org.slf4j.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;

/** Abstraction + polymorphism: two implementations of one interface, injected as a List. */
interface NotificationService { void notify(Long userId, String message); }
@Service class InAppNotificationService implements NotificationService {
  private final NotificationRepository repo; InAppNotificationService(NotificationRepository r){repo=r;}
  public void notify(Long u,String m){ repo.save(new Notification(u,m)); } }
@Service class EmailNotificationService implements NotificationService {
  private static final Logger log=LoggerFactory.getLogger("email");
  public void notify(Long u,String m){ log.info("[EMAIL STUB - not actually sent] to user {}: {}",u,m); } }

interface RegistrationService {
  Registration register(Long eventId, Long studentId, boolean allowWaitlist);
  void cancel(Long eventId, Long studentId);
  Path exportCsv(Long eventId) throws IOException; }

@Service class RegistrationServiceImpl implements RegistrationService {
  private final EventRepository events; private final StudentRepository students; private final RegistrationRepository regs;
  private final List<NotificationService> notifiers;
  RegistrationServiceImpl(EventRepository e,StudentRepository s,RegistrationRepository r,List<NotificationService> n){events=e;students=s;regs=r;notifiers=n;}
  private void tell(Long u,String m){ notifiers.forEach(n->n.notify(u,m)); }

  @Transactional public Registration register(Long eventId, Long studentId, boolean allowWaitlist){
    Event ev = events.lockById(eventId).orElseThrow(()->new CampusException(404,"Event not found")); // row lock => no overbooking
    Student st = students.findById(studentId).orElseThrow(()->new CampusException(404,"Student not found"));
    ev.assertOpenForRegistration(LocalDateTime.now());
    Optional<Registration> existing = regs.findByEventIdAndStudentId(eventId, studentId);
    if(existing.isPresent() && existing.get().getStatus()!=RegStatus.CANCELLED) throw new CampusException(409,"Already registered");
    boolean full = regs.countByEventIdAndStatus(eventId, RegStatus.CONFIRMED) >= ev.getCapacity();
    if(full && !allowWaitlist) throw new CampusException(409,"Event is full. Join the waitlist instead.");
    Registration r = existing.orElseGet(()->new Registration(ev, st, RegStatus.CONFIRMED));
    r.setStatusForRegister(full ? RegStatus.WAITLISTED : RegStatus.CONFIRMED);
    r = regs.save(r);
    tell(studentId, (full?"Waitlisted for ":"Registered for ")+ev.getTitle());
    return r; }

  @Transactional public void cancel(Long eventId, Long studentId){
    Event ev = events.lockById(eventId).orElseThrow(()->new CampusException(404,"Event not found"));
    Registration r = regs.findByEventIdAndStudentId(eventId, studentId).filter(x->x.getStatus()!=RegStatus.CANCELLED)
        .orElseThrow(()->new CampusException(404,"No active registration"));
    if(LocalDateTime.now().isAfter(ev.getStartTime())) throw new CampusException(409,"Event already started");
    boolean wasConfirmed = r.getStatus()==RegStatus.CONFIRMED;
    r.cancel(); regs.save(r); tell(studentId,"Cancelled registration for "+ev.getTitle());
    if(wasConfirmed){ // promote earliest waitlisted (FIFO by registeredAt)
      Queue<Registration> q = new ArrayDeque<>(regs.findByEventIdAndStatusOrderByRegisteredAtAsc(eventId, RegStatus.WAITLISTED));
      Registration next = q.poll();
      if(next!=null){ next.promote(); regs.save(next); tell(next.getStudent().getId(),"You got a seat for "+ev.getTitle()+"!"); } } }

  public Path exportCsv(Long eventId) throws IOException {
    Path p = Files.createTempFile("participants-"+eventId+"-", ".csv");
    try(BufferedWriter w = Files.newBufferedWriter(p)){
      w.write("name,email,status,registeredAt\n");
      for(Registration r: regs.findByEventId(eventId))
        w.write(String.join(",", r.getStudent().getName().replace(",", " "), r.getStudent().getEmail(), r.getStatus().name(), r.getRegisteredAt().toString())+"\n"); }
    return p; }
}
