package cz.fit.tsm.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.ServerOperationResponse;
import cz.fit.tsm.model.Course;
import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationCourse;
import cz.fit.tsm.repository.CourseRepository;
import cz.fit.tsm.repository.ServerOperationCourseRepository;
import cz.fit.tsm.repository.ServerOperationRepository;

@Service
public class ServerOperationService {

	private static final Logger log = LoggerFactory.getLogger(ServerOperationService.class);

	private final ServerOperationRepository serverOperationRepository;
	private final ServerOperationCourseRepository serverOperationCourseRepository;
	private final CourseRepository courseRepository;

	public ServerOperationService(ServerOperationRepository serverOperationRepository,
			ServerOperationCourseRepository serverOperationCourseRepository,
			CourseRepository courseRepository) {
		this.serverOperationRepository = serverOperationRepository;
		this.serverOperationCourseRepository = serverOperationCourseRepository;
		this.courseRepository = courseRepository;
	}

	@Transactional(readOnly = true)
	public List<ServerOperationResponse> listForRequester(UUID requesterId) {
		return serverOperationRepository.findByRequestedByOrderByCreatedAtDesc(requesterId).stream()
				.map(ServerOperationResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ServerOperation requireOwned(UUID operationId, UUID requesterId) {
		return serverOperationRepository.findByIdAndRequestedBy(operationId, requesterId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
	}

	@Transactional
	public void delete(UUID operationId, UUID requesterId) {
		ServerOperation operation = requireOwned(operationId, requesterId);
		assertEditable(operation);

		Course linkedCourse = serverOperationCourseRepository.findByOperationId(operationId)
				.map(ServerOperationCourse::getCourseId)
				.flatMap(courseId -> courseRepository.findByIdAndTeacherId(courseId, requesterId))
				.orElse(null);

		if (linkedCourse != null && linkedCourse.isSynced()) {
			log.warn("delete: blocked synced course={} op={} requester={}",
					linkedCourse.getId(), operationId, requesterId);
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Cannot remove a request for a synced course");
		}

		serverOperationRepository.delete(operation);
		if (linkedCourse != null) {
			courseRepository.delete(linkedCourse);
			log.info("delete: removed op={} and course={} requester={}",
					operationId, linkedCourse.getId(), requesterId);
		}
		else {
			log.info("delete: removed op={} requester={}", operationId, requesterId);
		}
	}

	static void assertEditable(ServerOperation operation) {
		if (operation.isCompleted()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed operations cannot be changed");
		}
	}
}
