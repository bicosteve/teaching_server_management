package cz.fit.tsm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
		@NotBlank @Size(max = 200) String name,
		@Size(max = 2000) String description,
		@NotBlank @Size(max = 32) String academicYear) {
}
