package cz.fit.tsm.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "server_operations")
public class ServerOperation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ServerOperationType type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> payload = new HashMap<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ServerOperationStatus status;

	@Column(name = "requested_by", nullable = false)
	private UUID requestedBy;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "executed_at")
	private Instant executedAt;

	@Column(name = "execution_result")
	private String executionResult;

	protected ServerOperation() {
	}

	public ServerOperation(ServerOperationType type, Map<String, Object> payload, ServerOperationStatus status,
			UUID requestedBy) {
		this.type = type;
		this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
		this.status = status;
		this.requestedBy = requestedBy;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public ServerOperationType getType() {
		return type;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public ServerOperationStatus getStatus() {
		return status;
	}

	public UUID getRequestedBy() {
		return requestedBy;
	}

	public UUID getApprovedBy() {
		return approvedBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getApprovedAt() {
		return approvedAt;
	}

	public Instant getExecutedAt() {
		return executedAt;
	}

	public String getExecutionResult() {
		return executionResult;
	}

	public boolean isCompleted() {
		return status == ServerOperationStatus.COMPLETED;
	}

	public void replacePayload(Map<String, Object> newPayload) {
		this.payload = newPayload != null ? new HashMap<>(newPayload) : new HashMap<>();
	}

	public void markPending() {
		this.status = ServerOperationStatus.PENDING;
		this.approvedBy = null;
		this.approvedAt = null;
		this.executedAt = null;
		this.executionResult = null;
	}
}
