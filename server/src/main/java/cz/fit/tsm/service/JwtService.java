package cz.fit.tsm.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cz.fit.tsm.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtService.class);

	public static final String TOKEN_TYPE_ACCESS = "access";
	public static final String TOKEN_TYPE_REFRESH = "refresh";

	private final SecretKey secretKey;
	private final long accessExpirationMs;
	private final long refreshExpirationMs;

	public JwtService(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms}") long accessExpirationMs,
			@Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessExpirationMs = accessExpirationMs;
		this.refreshExpirationMs = refreshExpirationMs;
		log.info("init: access TTL {} ms, refresh TTL {} ms", accessExpirationMs, refreshExpirationMs);
	}

	public String generateAccessToken(User user) {
		return buildToken(user, TOKEN_TYPE_ACCESS, accessExpirationMs);
	}

	public String generateRefreshToken(User user) {
		return buildToken(user, TOKEN_TYPE_REFRESH, refreshExpirationMs);
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public String extractRefreshUsername(String token) {
		Claims claims = parseClaims(token);
		if (!TOKEN_TYPE_REFRESH.equals(claims.get("typ", String.class))) {
			throw new IllegalArgumentException("Not a refresh token");
		}
		return claims.getSubject();
	}

	public boolean isAccessTokenValid(String token, String expectedUsername) {
		try {
			Claims claims = parseClaims(token);
			return TOKEN_TYPE_ACCESS.equals(claims.get("typ", String.class))
					&& expectedUsername.equals(claims.getSubject())
					&& claims.getExpiration().after(new Date());
		}
		catch (ExpiredJwtException ex) {
			return false;
		}
	}

	private String buildToken(User user, String type, long expirationMs) {
		Date now = new Date();
		Date expiresAt = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(user.getUsername())
				.claim("userId", user.getId().toString())
				.claim("role", user.getRole().name())
				.claim("typ", type)
				.issuedAt(now)
				.expiration(expiresAt)
				.signWith(secretKey)
				.compact();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
