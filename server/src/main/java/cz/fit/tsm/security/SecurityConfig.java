package cz.fit.tsm.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * REST API: Bearer JWT only, no servlet session / JSESSIONID.
	 */
	@Bean
	@Order(1)
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
			throws Exception {
		http
				.securityMatcher("/api/**")
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
						.requestMatchers(path("/api/teacher"), path("/api/teacher/**")).hasRole("TEACHER")
						.requestMatchers(path("/api/admin"), path("/api/admin/**")).hasRole("ADMIN")
						.requestMatchers(path("/api/student"), path("/api/student/**")).hasRole("STUDENT")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((req, res, e) -> res.sendError(HttpStatus.UNAUTHORIZED.value()))
						.accessDeniedHandler((req, res, e) -> res.sendError(HttpStatus.FORBIDDEN.value())))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * Browser / Faces UI: HTTP session cookies.
	 */
	@Bean
	@Order(2)
	SecurityFilterChain browserSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/jakarta.faces.resource/**", "/webjars/**", "/login", "/login.xhtml").permitAll()
						.requestMatchers("/not-found", "/not-found.xhtml", "/error").permitAll()
						.requestMatchers("/forbidden", "/forbidden.xhtml").authenticated()
						.requestMatchers(HttpMethod.GET, "/", "/index", "/index.xhtml").permitAll()
						.requestMatchers(path("/teacher"), path("/teacher/**")).hasRole("TEACHER")
						.requestMatchers(path("/admin"), path("/admin/**")).hasRole("ADMIN")
						.requestMatchers(path("/student"), path("/student/**")).hasRole("STUDENT")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((req, res, e) -> res.sendRedirect(req.getContextPath() + "/login"))
						.accessDeniedHandler((req, res, e) -> res.sendRedirect(req.getContextPath() + "/forbidden")));

		return http.build();
	}

	private static PathPatternRequestMatcher path(String pattern) {
		return PathPatternRequestMatcher.pathPattern(pattern);
	}
}
