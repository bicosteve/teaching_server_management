package cz.fit.tsm.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.AuthResponse;
import cz.fit.tsm.dto.LoginRequest;
import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.model.User;
import cz.fit.tsm.model.UserStatus;
import cz.fit.tsm.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String identifier = request.usernameOrEmail().trim();
		User user = userRepository.findByUsernameOrEmail(identifier, identifier.toLowerCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
		}

		return AuthResponse.bearer(jwtService.generateToken(user), UserResponse.from(user));
	}

	@Transactional(readOnly = true)
	public UserResponse currentUser(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		return UserResponse.from(user);
	}
}
