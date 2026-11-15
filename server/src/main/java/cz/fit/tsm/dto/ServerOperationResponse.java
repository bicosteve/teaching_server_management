package cz.fit.tsm.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationStatus;
import cz.fit.tsm.model.ServerOperationType;

public record ServerOperationResponse(
		UUID id,
		ServerOperationType type,
		ServerOperationStatus status,
		String summary,
		Instant createdAt,
		Instant approvedAt,
		Instant executedAt) {

	public static ServerOperationResponse from(ServerOperation operation) {
		Map<String, Object> payload = operation.getPayload();
		return new ServerOperationResponse(
				operation.getId(),
				operation.getType(),
				operation.getStatus(),
				stringValue(payload.get("name")),
				operation.getCreatedAt(),
				operation.getApprovedAt(),
				operation.getExecutedAt());
	}

	public boolean editable() {
		return status != ServerOperationStatus.COMPLETED;
	}

	private static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
