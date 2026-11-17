package cz.fit.tsm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.fit.tsm.model.Course;

public interface CourseRepository extends JpaRepository<Course, UUID> {

	List<Course> findByTeacherIdAndArchivedAtIsNullOrderByNameAsc(UUID teacherId);

	Optional<Course> findByIdAndTeacherId(UUID id, UUID teacherId);

	boolean existsByNameAndAcademicYear(String name, String academicYear);

	boolean existsByNameAndAcademicYearAndIdNot(String name, String academicYear, UUID id);
}