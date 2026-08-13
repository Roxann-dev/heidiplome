package hei.school.graduation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hei.school.graduation.security.CustomUserDetailsService;
import hei.school.graduation.security.JwtAuthenticationFilter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        .requestMatchers("/auth/**").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/promotions/**").hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.GET, "/groups/*/students").hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/semestres/*/groups").hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/courses").hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/courses/*/groups").hasRole("ADMIN")
                                        .requestMatchers("/users/**").hasRole("ADMIN")
                                        .requestMatchers("/teacher-course-assignments/**").hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/courses/*/examens")
                                        .hasAnyRole("ADMIN", "TEACHER")
                                        .requestMatchers(HttpMethod.GET, "/examens/*/notes")
                                        .hasAnyRole("ADMIN", "TEACHER")
                                        .requestMatchers(HttpMethod.POST, "/examens/*/notes")
                                        .hasAnyRole("ADMIN", "TEACHER")
                                        .requestMatchers(HttpMethod.PATCH, "/notes/*")
                                        .hasAnyRole("ADMIN", "TEACHER")
                                        .requestMatchers(HttpMethod.GET, "/courses/**")
                                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                                        .requestMatchers(HttpMethod.GET, "/notes/*/historique")
                                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                                        .requestMatchers(HttpMethod.GET, "/students/*/releves/**")
                                        .hasAnyRole("ADMIN", "STUDENT")
                                        .requestMatchers(HttpMethod.POST, "/students/*/releve-pdf")
                                        .hasAnyRole("ADMIN", "STUDENT")
                                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(
                        eh ->
                                eh.authenticationEntryPoint(
                                                (req, res, ex) -> writeJsonError(res, HttpStatus.UNAUTHORIZED, "Non authentifié"))
                                        .accessDeniedHandler(
                                                (req, res, ex) -> writeJsonError(res, HttpStatus.FORBIDDEN, "Accès refusé")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(
            jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response
                .getWriter()
                .write(
                        objectMapper.writeValueAsString(
                                Map.of(
                                        "status", status.value(),
                                        "message", message,
                                        "timestamp", java.time.LocalDateTime.now().toString())));
    }
}