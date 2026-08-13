package hei.school.graduation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  @Value("${jwt.secret}")
  private String secretKeyBase64;

  @Value("${jwt.expiration-ms}")
  private long expirationMs;

  public String generateToken(UserPrincipal principal) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", principal.getId().toString());
    claims.put("role", principal.getRole().name());

    return Jwts.builder()
        .claims(claims)
        .subject(principal.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(getSigningKey())
        .compact();
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public UUID extractUserId(String token) {
    String raw = extractAllClaims(token).get("userId", String.class);
    return UUID.fromString(raw);
  }

  public boolean isTokenValid(String token, UserPrincipal principal) {
    String username = extractUsername(token);
    return username.equals(principal.getUsername()) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  private <T> T extractClaim(String token, Function<Claims, T> resolver) {
    return resolver.apply(extractAllClaims(token));
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith((javax.crypto.SecretKey) getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(secretKeyBase64.getBytes());
  }
}
