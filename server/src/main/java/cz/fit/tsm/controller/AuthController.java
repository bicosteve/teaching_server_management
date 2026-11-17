package cz.fit.tsm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.fit.tsm.dto.AuthResponse;
import cz.fit.tsm.dto.LoginRequest;
import cz.fit.tsm.dto.RefreshRequest;
import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.security.UserPrincipal;
import cz.fit.tsm.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		log.info("login: issuing bearer tokens");
		return authService.login(request);
	}

	@PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
	public AuthResponse refresh(@Valid @RequestBody RefreshRequest body) {
		log.info("refresh: refreshing bearer tokens");
		return authService.refresh(body.refreshToken());
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("me: returning current user");
		return authService.currentUser(principal.getUsername());
	}
}
