package cz.fit.tsm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public User authenticate(LoginRequest request) {
		String identifier = request.usernameOrEmail().trim();
		User user = userRepository.findByUsernameOrEmail(identifier, identifier.toLowerCase())
				.orElseThrow(() -> {
					log.warn("authenticate: unknown identity");
					return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
				});

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			log.warn("authenticate: bad password for user={}", user.getUsername());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			log.warn("authenticate: inactive account user={} status={}", user.getUsername(), user.getStatus());
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
		}
		log.info("authenticate: ok user={} role={}", user.getUsername(), user.getRole());
		return user;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		User user = authenticate(request);
		log.info("login: issuing tokens user={}", user.getUsername());
		return issueTokens(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			log.warn("refresh: missing token");
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token required");
		}
		try {
			String username = jwtService.extractRefreshUsername(refreshToken);
			User user = userRepository.findByUsername(username)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
			if (user.getStatus() != UserStatus.ACTIVE) {
				log.warn("refresh: inactive account user={}", username);
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
			}
			log.info("refresh: issuing tokens user={}", username);
			return issueTokens(user);
		}
		catch (ResponseStatusException ex) {
			throw ex;
		}
		catch (Exception ex) {
			log.warn("refresh: invalid token ({})", ex.getClass().getSimpleName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}
	}

	@Transactional(readOnly = true)
	public UserResponse currentUser(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> {
					log.warn("currentUser: not found user={}", username);
					return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
				});
		return UserResponse.from(user);
	}

	private AuthResponse issueTokens(User user) {
		return AuthResponse.bearer(
				jwtService.generateAccessToken(user),
				jwtService.generateRefreshToken(user),
				UserResponse.from(user));
	}
}
