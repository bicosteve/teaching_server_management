package cz.fit.tsm.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cz.fit.tsm.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates API requests that send {@code Authorization: Bearer <access-jwt>}.
 * Browser/Faces traffic relies on the HTTP session instead.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getServletPath().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String accessToken = resolveBearerToken(request);
		if (accessToken != null && !isLoggedIn()) {
			authenticateWithAccessToken(accessToken);
		}
		filterChain.doFilter(request, response);
	}

	private void authenticateWithAccessToken(String accessToken) {
		try {
			String username = jwtService.extractUsername(accessToken);
			UserDetails user = userDetailsService.loadUserByUsername(username);
			if (jwtService.isAccessTokenValid(accessToken, user.getUsername()) && user.isEnabled()) {
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
				log.debug("bearer: authenticated user={}", username);
			}
			else {
				log.warn("bearer: rejected token for user={}", username);
				SecurityContextHolder.clearContext();
			}
		}
		catch (Exception ex) {
			log.warn("bearer: invalid access token ({})", ex.getClass().getSimpleName());
			SecurityContextHolder.clearContext();
		}
	}

	private static boolean isLoggedIn() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
	}

	private static String resolveBearerToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			String bearer = header.substring(7);
			return bearer.isBlank() ? null : bearer;
		}
		return null;
	}
}
