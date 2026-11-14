package cz.fit.tsm.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		return ResponseEntity.status(status).body(error(status, ex.getReason(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Validation failed");
		return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
	}

	private static ApiError error(HttpStatus status, String message, String path) {
		return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
	}
}
