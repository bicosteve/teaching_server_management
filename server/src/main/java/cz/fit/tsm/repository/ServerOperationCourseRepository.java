package cz.fit.tsm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.fit.tsm.model.ServerOperationCourse;

public interface ServerOperationCourseRepository extends JpaRepository<ServerOperationCourse, UUID> {

	Optional<ServerOperationCourse> findByOperationId(UUID operationId);

	List<ServerOperationCourse> findAllByCourseId(UUID courseId);
}
