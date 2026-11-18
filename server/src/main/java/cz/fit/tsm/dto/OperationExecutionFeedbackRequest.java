package cz.fit.tsm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OperationExecutionFeedbackRequest(
		@NotNull Boolean success,
		@Size(max = 1000) String executionResult) {
}
