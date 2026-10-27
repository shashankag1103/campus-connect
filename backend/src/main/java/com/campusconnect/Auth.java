package com.campusconnect;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import javax.crypto.SecretKey;
// ─── JWT Utility ────────────────────────────────────────────────────────────
@Component
class JwtUtil {
  private final SecretKey key;
  private final long expiryMs;
  JwtUtil(@Value("${jwt.secret}") String secret,
          @Value("${jwt.expiry-days:7}") int days) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiryMs = (long) days * 24 * 60 * 60 * 1000;
  }
  String generate(Long userId, String role) {
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("role", role)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusMillis(expiryMs)))
        .signWith(key)
        .compact();
  }
  Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build()
        .parseSignedClaims(token).getPayload();
  }
  Long userId(String token) { return Long.valueOf(parse(token).getSubject()); }
  String role(String token)  { return (String) parse(token).get("role"); }
}
// ─── Auth Filter (HandlerInterceptor) ───────────────────────────────────────
@Component
class AuthInterceptor implements HandlerInterceptor {
  static final String ATTR = "currentUser";
  private final JwtUtil jwt;
  private final StudentRepository students;
  private final OrganizerRepository organizers;
  AuthInterceptor(JwtUtil j, StudentRepository s, OrganizerRepository o){
    jwt=j; students=s; organizers=o;
  }
  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object h) {
    String header = req.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        String token = header.substring(7);
        Long uid = jwt.userId(token);
        String role = jwt.role(token);
        BaseUser user = "ORGANIZER".equals(role)
            ? organizers.findById(uid).orElse(null)
            : students.findById(uid).orElse(null);
        if (user != null) req.setAttribute(ATTR, user);
      } catch (Exception ignored) { /* invalid token – user stays null */ }
    }
    return true; // always continue; endpoints themselves enforce auth
  }
}
// ─── Auth Controller ─────────────────────────────────────────────────────────
record SignUpRequest(String name, String email, String password, String studentNumber, String role, String club) {}
record SignInRequest(String email, String password) {}
record UserView(Long id, String name, String email, String role, String token) {}
@RestController @RequestMapping("/api/auth")
class AuthController {
  private final StudentRepository students;
  private final OrganizerRepository organizers;
  private final JwtUtil jwt;
  AuthController(StudentRepository s, OrganizerRepository o, JwtUtil j){ students=s; organizers=o; jwt=j; }
  private UserView toView(BaseUser u, String token){
    return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole(), token);
  }
  @PostMapping("/signup")
  UserView signup(@RequestBody SignUpRequest req) {
    if (req.email()==null||req.email().isBlank()) throw new CampusException(400,"Email is required");
    if (req.password()==null||req.password().length()<8) throw new CampusException(400,"Password must be at least 8 characters");
    String email = req.email().toLowerCase().trim();
    // check uniqueness across both tables
    if (students.findByEmail(email).isPresent() || organizers.findByEmail(email).isPresent())
      throw new CampusException(409,"An account with that email already exists");
    boolean isOrg = "ORGANIZER".equalsIgnoreCase(req.role());
    BaseUser saved;
    if (isOrg) {
      String club = (req.club()!=null&&!req.club().isBlank()) ? req.club().trim() : req.name().trim();
      Organizer o = new Organizer(req.name(), email, club);
      o.setPassword(req.password());
      saved = organizers.save(o);
    } else {
      String sn = (req.studentNumber()!=null&&!req.studentNumber().isBlank()) ? req.studentNumber().trim() : "";
      Student s = new Student(req.name(), email, sn);
      s.setPassword(req.password());
      saved = students.save(s);
    }
    return toView(saved, jwt.generate(saved.getId(), saved.getRole()));
  }
  @PostMapping("/signin")
  UserView signin(@RequestBody SignInRequest req) {
    if (req.email()==null||req.password()==null) throw new CampusException(400,"Email and password required");
    String email = req.email().toLowerCase().trim();
    BaseUser user = students.findByEmail(email)
        .<BaseUser>map(u->u)
        .or(() -> organizers.findByEmail(email).map(u->u))
        .orElseThrow(() -> new CampusException(401,"Invalid email or password"));
    if (!user.checkPassword(req.password()))
      throw new CampusException(401,"Invalid email or password");
    return toView(user, jwt.generate(user.getId(), user.getRole()));
  }
  @GetMapping("/me")
  UserView me(HttpServletRequest req) {
    BaseUser u = (BaseUser) req.getAttribute(AuthInterceptor.ATTR);
    if (u==null) throw new CampusException(401,"Not authenticated");
    return toView(u, null); // token not re-issued on /me
  }
}
// ─── WebMvcConfigurer that registers the interceptor ─────────────────────────
// Note: CampusConnectApplication already has a @Bean WebMvcConfigurer for CORS.
// We use a separate one here to keep concerns separated.
@Component
class AuthWebConfig implements WebMvcConfigurer {
  private final AuthInterceptor interceptor;
  AuthWebConfig(AuthInterceptor i){ interceptor=i; }
  @Override public void addInterceptors(InterceptorRegistry r){
    r.addInterceptor(interceptor).addPathPatterns("/api/**");
  }
}
