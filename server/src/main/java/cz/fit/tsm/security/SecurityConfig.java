package cz.fit.tsm.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
			throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/jakarta.faces.resource/**", "/webjars/**", "/login", "/login.xhtml").permitAll()
						.requestMatchers("/not-found", "/not-found.xhtml", "/error").permitAll()
						.requestMatchers("/forbidden", "/forbidden.xhtml").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
						.requestMatchers(HttpMethod.GET, "/", "/index", "/index.xhtml").permitAll()
						.requestMatchers(
								path("/api/teacher"), path("/api/teacher/**"),
								path("/teacher"), path("/teacher/**"))
						.hasRole("TEACHER")
						.requestMatchers(
								path("/api/admin"), path("/api/admin/**"),
								path("/admin"), path("/admin/**"))
						.hasRole("ADMIN")
						.requestMatchers(
								path("/api/student"), path("/api/student/**"),
								path("/student"), path("/student/**"))
						.hasRole("STUDENT")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((req, res, e) -> {
							if (isApi(req)) {
								res.sendError(HttpStatus.UNAUTHORIZED.value());
							}
							else {
								res.sendRedirect(req.getContextPath() + "/login");
							}
						})
						.accessDeniedHandler((req, res, e) -> {
							if (isApi(req)) {
								res.sendError(HttpStatus.FORBIDDEN.value());
							}
							else {
								res.sendRedirect(req.getContextPath() + "/forbidden");
							}
						}))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private static PathPatternRequestMatcher path(String pattern) {
		return PathPatternRequestMatcher.pathPattern(pattern);
	}

	private static boolean isApi(HttpServletRequest request) {
		return request.getServletPath().startsWith("/api/");
	}
}
