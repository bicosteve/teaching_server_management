package cz.fit.tsm.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cz.fit.tsm.dto.AuthResponse;
import cz.fit.tsm.dto.LoginRequest;
import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.security.UserPrincipal;
import cz.fit.tsm.service.AuthService;
import jakarta.validation.Valid;

@RestController
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping(path = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/api/auth/me")
	public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		return authService.currentUser(principal.getUsername());
	}
}
