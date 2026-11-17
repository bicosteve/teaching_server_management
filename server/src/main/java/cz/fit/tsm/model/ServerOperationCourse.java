package cz.fit.tsm.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "server_operation_courses")
public class ServerOperationCourse {

	@Id
	@Column(name = "operation_id", nullable = false)
	private UUID operationId;

	@Column(name = "course_id", nullable = false)
	private UUID courseId;

	protected ServerOperationCourse() {
	}

	public ServerOperationCourse(UUID operationId, UUID courseId) {
		this.operationId = operationId;
		this.courseId = courseId;
	}

	public UUID getOperationId() {
		return operationId;
	}

	public UUID getCourseId() {
		return courseId;
	}
}
