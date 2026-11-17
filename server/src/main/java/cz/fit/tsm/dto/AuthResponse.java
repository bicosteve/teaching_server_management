package cz.fit.tsm.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		UserResponse user) {

	public static AuthResponse bearer(String accessToken, String refreshToken, UserResponse user) {
		return new AuthResponse(accessToken, refreshToken, "Bearer", user);
	}
}
