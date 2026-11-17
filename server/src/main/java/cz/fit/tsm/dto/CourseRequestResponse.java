package cz.fit.tsm.dto;

import java.time.Instant;
import java.util.UUID;

import cz.fit.tsm.model.Course;
import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationStatus;

public record CourseRequestResponse(
		UUID id,
		UUID courseId,
		String name,
		String description,
		String academicYear,
		ServerOperationStatus status,
		Instant createdAt) {

	public static CourseRequestResponse from(ServerOperation operation, Course course) {
		return new CourseRequestResponse(
				operation.getId(),
				course.getId(),
				course.getName(),
				course.getDescription(),
				course.getAcademicYear(),
				operation.getStatus(),
				operation.getCreatedAt());
	}
}
