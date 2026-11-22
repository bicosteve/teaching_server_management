package cz.fit.tsm.dto;

import java.util.UUID;

import cz.fit.tsm.model.Course;
import cz.fit.tsm.model.CourseStatus;

public record CourseResponse(
		UUID id,
		String name,
		String description,
		String academicYear,
		CourseStatus status) {

	public static CourseResponse from(Course course) {
		return new CourseResponse(
				course.getId(),
				course.getName(),
				course.getDescription(),
				course.getAcademicYear(),
				course.getStatus());
	}

	public String statusSeverity() {
		return switch (status) {
			case PENDING -> "warn";
			case APPROVED -> "info";
			case SYNCED -> "success";
			case REJECTED, FAILED -> "danger";
		};
	}

	public String statusTooltipKey() {
		return "course.status." + status.name();
	}
}
