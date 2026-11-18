package cz.fit.tsm.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.ServerOperationResponse;
import cz.fit.tsm.model.Course;
import cz.fit.tsm.model.CourseStatus;
import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationCourse;
import cz.fit.tsm.model.ServerOperationStatus;
import cz.fit.tsm.model.ServerOperationType;
import cz.fit.tsm.model.User;
import cz.fit.tsm.repository.CourseRepository;
import cz.fit.tsm.repository.ServerOperationCourseRepository;
import cz.fit.tsm.repository.ServerOperationRepository;
import cz.fit.tsm.repository.UserRepository;

@Service
public class ServerOperationService {

	private static final Logger log = LoggerFactory.getLogger(ServerOperationService.class);

	private final ServerOperationRepository serverOperationRepository;
	private final ServerOperationCourseRepository serverOperationCourseRepository;
	private final CourseRepository courseRepository;
	private final UserRepository userRepository;

	public ServerOperationService(ServerOperationRepository serverOperationRepository,
			ServerOperationCourseRepository serverOperationCourseRepository,
			CourseRepository courseRepository,
			UserRepository userRepository) {
		this.serverOperationRepository = serverOperationRepository;
		this.serverOperationCourseRepository = serverOperationCourseRepository;
		this.courseRepository = courseRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<ServerOperationResponse> listAll() {
		return toResponses(serverOperationRepository.findAllByOrderByCreatedAtDesc());
	}

	@Transactional(readOnly = true)
	public List<ServerOperationResponse> listByStatus(ServerOperationStatus status) {
		return toResponses(serverOperationRepository.findByStatusOrderByCreatedAtDesc(status));
	}

	@Transactional(readOnly = true)
	public ServerOperationResponse get(UUID operationId) {
		return toResponses(List.of(require(operationId))).getFirst();
	}

	@Transactional(readOnly = true)
	public List<ServerOperationResponse> listForRequester(UUID requesterId) {
		return toResponses(serverOperationRepository.findByRequestedByOrderByCreatedAtDesc(requesterId));
	}

	@Transactional(readOnly = true)
	public List<ServerOperationResponse> listForRequester(UUID requesterId, ServerOperationStatus status) {
		return toResponses(serverOperationRepository.findByRequestedByAndStatusOrderByCreatedAtDesc(requesterId, status));
	}

	@Transactional(readOnly = true)
	public ServerOperation require(UUID operationId) {
		return serverOperationRepository.findById(operationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
	}

	@Transactional(readOnly = true)
	public ServerOperation requireOwned(UUID operationId, UUID requesterId) {
		return serverOperationRepository.findByIdAndRequestedBy(operationId, requesterId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
	}

	@Transactional
	public void approve(UUID operationId, UUID adminId) {
		ServerOperation operation = require(operationId);
		assertPending(operation);
		operation.markApproved(adminId);
		serverOperationRepository.save(operation);
		syncLinkedCourseStatus(operationId, CourseStatus.APPROVED);
		log.info("approve: op={} admin={}", operationId, adminId);
	}

	@Transactional
	public void reject(UUID operationId, UUID adminId) {
		ServerOperation operation = require(operationId);
		assertPending(operation);
		operation.markRejected();
		serverOperationRepository.save(operation);
		syncLinkedCourseStatus(operationId, CourseStatus.REJECTED);
		log.info("reject: op={} admin={}", operationId, adminId);
	}

	@Transactional
	public void delete(UUID operationId, UUID requesterId) {
		ServerOperation operation = requireOwned(operationId, requesterId);
		assertEditable(operation);
		Course course = linkedCourse(operationId, requesterId);

		if (operation.getType() == ServerOperationType.CREATE_COURSE) {
			if (course != null && course.isSynced()) {
				log.warn("delete: blocked synced course={} op={} requester={}", course.getId(), operationId, requesterId);
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove a request for a synced course");
			}
			serverOperationRepository.delete(operation);
			if (course != null) {
				courseRepository.delete(course);
			}
			log.info("delete: removed create op={} course={} requester={}",
					operationId, course != null ? course.getId() : null, requesterId);
			return;
		}

		serverOperationRepository.delete(operation);
		if (operation.getType() == ServerOperationType.UPDATE_COURSE
				&& course != null
				&& course.isPending()
				&& !hasOtherPendingOps(course.getId(), operationId)) {
			course.markSynced();
			courseRepository.save(course);
		}
		log.info("delete: removed op={} type={} requester={}", operationId, operation.getType(), requesterId);
	}

	static void assertEditable(ServerOperation operation) {
		if (operation.isCompleted()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed operations cannot be changed");
		}
	}

	static void assertPending(ServerOperation operation) {
		if (operation.getStatus() != ServerOperationStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending operations can be decided");
		}
	}

	private List<ServerOperationResponse> toResponses(List<ServerOperation> operations) {
		Map<UUID, User> users = userRepository.findAllById(userIds(operations)).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		Map<UUID, String> courseNames = loadLinkedCourseNames(operations);
		return operations.stream()
				.map(op -> ServerOperationResponse.from(
						op,
						displayName(users.get(op.getRequestedBy())),
						displayName(users.get(op.getApprovedBy())),
						courseNames.get(op.getId())))
				.toList();
	}

	private void syncLinkedCourseStatus(UUID operationId, CourseStatus status) {
		Course course = linkedCourse(operationId);
		if (course == null) {
			return;
		}
		if (status == CourseStatus.APPROVED) {
			course.markApproved();
		}
		else if (status == CourseStatus.REJECTED) {
			course.markRejected();
		}
		courseRepository.save(course);
	}

	private Course linkedCourse(UUID operationId) {
		return serverOperationCourseRepository.findByOperationId(operationId)
				.map(ServerOperationCourse::getCourseId)
				.flatMap(courseRepository::findById)
				.orElse(null);
	}

	private Course linkedCourse(UUID operationId, UUID requesterId) {
		return serverOperationCourseRepository.findByOperationId(operationId)
				.map(ServerOperationCourse::getCourseId)
				.flatMap(courseId -> courseRepository.findByIdAndTeacherId(courseId, requesterId))
				.orElse(null);
	}

	private boolean hasOtherPendingOps(UUID courseId, UUID excludedOperationId) {
		return serverOperationCourseRepository.findAllByCourseId(courseId).stream()
				.map(ServerOperationCourse::getOperationId)
				.filter(id -> !id.equals(excludedOperationId))
				.map(serverOperationRepository::findById)
				.flatMap(Optional::stream)
				.anyMatch(op -> !op.isCompleted());
	}

	private Map<UUID, String> loadLinkedCourseNames(Collection<ServerOperation> operations) {
		if (operations.isEmpty()) {
			return Map.of();
		}
		List<ServerOperationCourse> links = serverOperationCourseRepository.findAllByOperationIdIn(
				operations.stream().map(ServerOperation::getId).toList());
		if (links.isEmpty()) {
			return Map.of();
		}
		Map<UUID, Course> courses = courseRepository.findAllById(
				links.stream().map(ServerOperationCourse::getCourseId).toList()).stream()
				.collect(Collectors.toMap(Course::getId, Function.identity()));
		return links.stream()
				.filter(link -> courses.containsKey(link.getCourseId()))
				.collect(Collectors.toMap(
						ServerOperationCourse::getOperationId,
						link -> courses.get(link.getCourseId()).getName(),
						(left, right) -> left));
	}

	private static Set<UUID> userIds(Collection<ServerOperation> operations) {
		Set<UUID> ids = new HashSet<>();
		for (ServerOperation operation : operations) {
			ids.add(operation.getRequestedBy());
			if (operation.getApprovedBy() != null) {
				ids.add(operation.getApprovedBy());
			}
		}
		return ids;
	}

	private static String displayName(User user) {
		return user == null ? null : user.getFirstName() + " " + user.getLastName();
	}
}
