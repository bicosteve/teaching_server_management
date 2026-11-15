package cz.fit.tsm.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationStatus;

public record CourseRequestResponse(
		UUID id,
		String name,
		String description,
		String academicYear,
		ServerOperationStatus status,
		Instant createdAt) {

	public static CourseRequestResponse from(ServerOperation operation) {
		Map<String, Object> payload = operation.getPayload();
		return new CourseRequestResponse(
				operation.getId(),
				stringValue(payload.get("name")),
				stringValue(payload.get("description")),
				stringValue(payload.get("academic_year")),
				operation.getStatus(),
				operation.getCreatedAt());
	}

	private static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
