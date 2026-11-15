package cz.fit.tsm.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "courses", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "academic_year" }))
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "teacher_id", nullable = false)
	private UUID teacherId;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	@Column(name = "academic_year", nullable = false)
	private String academicYear;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CourseStatus status;

	@Column(name = "archived_at")
	private Instant archivedAt;

	protected Course() {
	}

	public Course(UUID teacherId, String name, String description, String academicYear, CourseStatus status) {
		this.teacherId = teacherId;
		this.name = name;
		this.description = description;
		this.academicYear = academicYear;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTeacherId() {
		return teacherId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getAcademicYear() {
		return academicYear;
	}

	public CourseStatus getStatus() {
		return status;
	}

	public Instant getArchivedAt() {
		return archivedAt;
	}

	public boolean isSynced() {
		return status == CourseStatus.SYNCED;
	}

	public void updateDetails(String name, String description, String academicYear) {
		this.name = name;
		this.description = description;
		this.academicYear = academicYear;
		if (this.status == CourseStatus.FAILED) {
			this.status = CourseStatus.PENDING;
		}
	}
}
