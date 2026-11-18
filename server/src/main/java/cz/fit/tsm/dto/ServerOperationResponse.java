package cz.fit.tsm.dto;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationStatus;
import cz.fit.tsm.model.ServerOperationType;

public record ServerOperationResponse(
		UUID id,
		ServerOperationType type,
		ServerOperationStatus status,
		String target,
		UUID requestedBy,
		String requestedByName,
		UUID approvedBy,
		String approvedByName,
		Map<String, Object> payload,
		Instant createdAt,
		Instant approvedAt,
		Instant executedAt,
		String executionResult) {

	public record PayloadField(String key, String label, String value) {
	}

	public record MetaField(String labelKey, Object value) {
	}

	public static ServerOperationResponse from(ServerOperation operation, String requestedByName,
			String approvedByName, String linkedCourseName) {
		Map<String, Object> payload = Map.copyOf(operation.getPayload());
		String target = blankToNull(linkedCourseName);
		if (target == null) {
			target = stringValue(payload.get("name"));
		}
		return new ServerOperationResponse(
				operation.getId(),
				operation.getType(),
				operation.getStatus(),
				target,
				operation.getRequestedBy(),
				requestedByName,
				operation.getApprovedBy(),
				approvedByName,
				payload,
				operation.getCreatedAt(),
				operation.getApprovedAt(),
				operation.getExecutedAt(),
				operation.getExecutionResult());
	}

	public List<PayloadField> payloadFields() {
		return payload.entrySet().stream()
				.map(e -> new PayloadField(e.getKey(), humanizeKey(e.getKey()), stringValue(e.getValue())))
				.toList();
	}

	public List<MetaField> metaFields() {
		return List.of(
				new MetaField("admin.operations.target", target),
				new MetaField("admin.operations.requestedBy", requestedByName),
				new MetaField("admin.operations.approvedBy", approvedByName),
				new MetaField("admin.operations.createdAt", createdAt),
				new MetaField("admin.operations.approvedAt", approvedAt),
				new MetaField("admin.operations.executedAt", executedAt),
				new MetaField("admin.operations.executionResult", executionResult));
	}

	public String statusSeverity() {
		return switch (status) {
			case PENDING -> "warn";
			case APPROVED -> "info";
			case COMPLETED -> "success";
			case REJECTED, FAILED -> "danger";
		};
	}

	public String statusTooltipKey() {
		return "operation.status." + status.name();
	}

	public boolean editable() {
		return status != ServerOperationStatus.COMPLETED;
	}

	public boolean pending() {
		return status == ServerOperationStatus.PENDING;
	}

	private static String humanizeKey(String key) {
		if (key == null || key.isBlank()) {
			return key;
		}
		return Arrays.stream(key.replace('-', '_').split("_"))
				.filter(part -> !part.isEmpty())
				.map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
