package hei.school.graduation.endpoint.web.controller;

import hei.school.graduation.security.CustomUserDetailsService;
import hei.school.graduation.security.JwtService;
import hei.school.graduation.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

@Controller
@RequiredArgsConstructor
public class WebLoginController {

  private static final String JWT_COOKIE_NAME = "jwt_token";
  private static final int COOKIE_MAX_AGE = 86400;

  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;
  private final JwtService jwtService;

  @GetMapping("/login")
  public String loginPage(@RequestParam(required = false) String error, Model model) {
    if (error != null) {
      model.addAttribute("error", "Email ou mot de passe incorrect");
    }
    return "login";
  }

  @PostMapping("/login")
  public String login(
      @RequestParam String email,
      @RequestParam String password,
      HttpServletResponse response) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(email, password));
    } catch (AuthenticationException e) {
      return "redirect:/login?error";
    }

    UserPrincipal principal =
        (UserPrincipal) userDetailsService.loadUserByUsername(email);
    String token = jwtService.generateToken(principal);

    Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(COOKIE_MAX_AGE);
    response.addCookie(cookie);

    return "redirect:/promotions-view";
  }

  @GetMapping("/logout")
  public String logout(HttpServletResponse response) {
    Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
    return "redirect:/login";
  }
}
