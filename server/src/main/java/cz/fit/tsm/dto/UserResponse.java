package cz.fit.tsm.dto;

import java.time.Instant;
import java.util.UUID;

import cz.fit.tsm.model.User;
import cz.fit.tsm.model.UserRole;
import cz.fit.tsm.model.UserStatus;
import cz.fit.tsm.security.UserPrincipal;

public record UserResponse(
		UUID id,
		String firstName,
		String lastName,
		String email,
		String username,
		UserRole role,
		UserStatus status,
		Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				user.getUsername(),
				user.getRole(),
				user.getStatus(),
				user.getCreatedAt());
	}

	public static UserResponse from(UserPrincipal principal) {
		return new UserResponse(
				principal.getId(),
				principal.getFirstName(),
				principal.getLastName(),
				principal.getEmail(),
				principal.getUsername(),
				principal.getRole(),
				principal.getStatus(),
				principal.getCreatedAt());
	}
}
