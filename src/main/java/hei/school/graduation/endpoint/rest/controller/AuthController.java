package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.security.CustomUserDetailsService;
import hei.school.graduation.security.JwtService;
import hei.school.graduation.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;
  private final JwtService jwtService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    UserPrincipal principal =
        (UserPrincipal) userDetailsService.loadUserByUsername(request.email());
    String token = jwtService.generateToken(principal);
    return ResponseEntity.ok(new LoginResponse(token));
  }
}
