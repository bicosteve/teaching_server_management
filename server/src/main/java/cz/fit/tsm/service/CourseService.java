package cz.fit.tsm.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.CourseRequestResponse;
import cz.fit.tsm.dto.CourseResponse;
import cz.fit.tsm.dto.CreateCourseRequest;
import cz.fit.tsm.model.Course;
import cz.fit.tsm.model.CourseStatus;
import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationCourse;
import cz.fit.tsm.model.ServerOperationStatus;
import cz.fit.tsm.model.ServerOperationType;
import cz.fit.tsm.repository.CourseRepository;
import cz.fit.tsm.repository.ServerOperationCourseRepository;
import cz.fit.tsm.repository.ServerOperationRepository;

@Service
public class CourseService {

	private static final Logger log = LoggerFactory.getLogger(CourseService.class);

	private final CourseRepository courseRepository;
	private final ServerOperationRepository serverOperationRepository;
	private final ServerOperationCourseRepository serverOperationCourseRepository;
	private final ServerOperationService serverOperationService;

	public CourseService(CourseRepository courseRepository, ServerOperationRepository serverOperationRepository,
			ServerOperationCourseRepository serverOperationCourseRepository,
			ServerOperationService serverOperationService) {
		this.courseRepository = courseRepository;
		this.serverOperationRepository = serverOperationRepository;
		this.serverOperationCourseRepository = serverOperationCourseRepository;
		this.serverOperationService = serverOperationService;
	}

	@Transactional
	public CourseResponse requestCourse(UUID teacherId, CreateCourseRequest request) {
		String name = request.name().trim();
		String academicYear = request.academicYear().trim();
		String description = trimToNull(request.description());

		if (courseRepository.existsByNameAndAcademicYear(name, academicYear)) {
			log.warn("requestCourse: conflict name={} year={} teacher={}", name, academicYear, teacherId);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A course with this name and academic year already exists");
		}

		Course course = courseRepository.save(new Course(
				teacherId,
				name,
				description,
				academicYear,
				CourseStatus.PENDING));

		ServerOperation operation = saveLinkedOperation(new ServerOperation(
				ServerOperationType.CREATE_COURSE,
				coursePayload(name, description, academicYear),
				ServerOperationStatus.PENDING,
				teacherId), course.getId());
		log.info("requestCourse: created course={} op={} teacher={}", course.getId(), operation.getId(), teacherId);
		return CourseResponse.from(course);
	}

	@Transactional(readOnly = true)
	public List<CourseResponse> listCourses(UUID teacherId) {
		return courseRepository.findByTeacherIdAndArchivedAtIsNullOrderByNameAsc(teacherId).stream()
				.map(CourseResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public CourseResponse getCourse(UUID courseId, UUID teacherId) {
		return CourseResponse.from(requireOwnedCourse(courseId, teacherId));
	}

	@Transactional
	public CourseResponse updateCourse(UUID courseId, UUID teacherId, CreateCourseRequest request) {
		Course course = requireOwnedCourse(courseId, teacherId);
		String name = request.name().trim();
		String academicYear = request.academicYear().trim();
		String description = trimToNull(request.description());

		if (courseRepository.existsByNameAndAcademicYearAndIdNot(name, academicYear, course.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A course with this name and academic year already exists");
		}

		Optional<ServerOperation> pendingCreate = findPendingCreateCourseOp(courseId, teacherId);
		if (pendingCreate.isPresent()) {
			return patchPendingCreate(course, pendingCreate.get(), name, description, academicYear, teacherId);
		}

		Optional<ServerOperation> pendingUpdate = findPendingUpdateCourseOp(courseId, teacherId);
		SyncedBaseline synced = syncedBaseline(course, pendingUpdate);
		String previousDescription = course.getDescription();
		course.updateDetails(name, description, academicYear);

		if (!Objects.equals(previousDescription, description)) {
			recordDescriptionHistory(course, teacherId, synced, previousDescription, description);
		}
		syncPendingRename(course, teacherId, pendingUpdate, synced, name, description, academicYear);

		return CourseResponse.from(courseRepository.save(course));
	}

	private CourseResponse patchPendingCreate(Course course, ServerOperation operation, String name,
			String description, String academicYear, UUID teacherId) {
		course.updateDetails(name, description, academicYear);
		operation.replacePayload(coursePayload(name, description, academicYear));
		operation.markPending();
		serverOperationRepository.save(operation);
		log.info("updateCourse: patched pending create op={} course={} teacher={}",
				operation.getId(), course.getId(), teacherId);
		return CourseResponse.from(courseRepository.save(course));
	}

	private void recordDescriptionHistory(Course course, UUID teacherId, SyncedBaseline synced,
			String previousDescription, String description) {
		Map<String, Object> history = new LinkedHashMap<>();
		history.put("description", description);
		putPrevious(history, synced.name(), previousDescription, synced.academicYear());
		ServerOperation operation = new ServerOperation(
				ServerOperationType.UPDATE_COURSE,
				history,
				ServerOperationStatus.COMPLETED,
				teacherId);
		operation.markCompleted("Description updated");
		saveLinkedOperation(operation, course.getId());
		log.info("updateCourse: recorded description history op={} course={} teacher={}",
				operation.getId(), course.getId(), teacherId);
	}

	private void syncPendingRename(Course course, UUID teacherId, Optional<ServerOperation> pendingUpdate,
			SyncedBaseline synced, String name, String description, String academicYear) {
		boolean renameChanged = !Objects.equals(synced.name(), name)
				|| !Objects.equals(synced.academicYear(), academicYear);
		if (!renameChanged) {
			pendingUpdate.ifPresent(operation -> {
				serverOperationService.delete(operation.getId(), teacherId);
				log.info("updateCourse: cleared pending UPDATE_COURSE op={} course={} teacher={}",
						operation.getId(), course.getId(), teacherId);
			});
			return;
		}

		Map<String, Object> changedFields = changedCourseFields(
				synced.name(), synced.description(), synced.academicYear(), name, description, academicYear);
		course.markPending();
		if (pendingUpdate.isPresent()) {
			ServerOperation operation = pendingUpdate.get();
			operation.replacePayload(changedFields);
			operation.markPending();
			serverOperationRepository.save(operation);
			log.info("updateCourse: overwrote pending UPDATE_COURSE op={} fields={} course={} teacher={}",
					operation.getId(), changedFields.keySet(), course.getId(), teacherId);
			return;
		}

		ServerOperation operation = saveLinkedOperation(new ServerOperation(
				ServerOperationType.UPDATE_COURSE,
				changedFields,
				ServerOperationStatus.PENDING,
				teacherId), course.getId());
		log.info("updateCourse: enqueued UPDATE_COURSE op={} fields={} course={} teacher={}",
				operation.getId(), changedFields.keySet(), course.getId(), teacherId);
	}

	private static SyncedBaseline syncedBaseline(Course course, Optional<ServerOperation> pendingUpdate) {
		if (pendingUpdate.isEmpty()) {
			return new SyncedBaseline(course.getName(), course.getDescription(), course.getAcademicYear());
		}
		Map<String, Object> payload = pendingUpdate.get().getPayload();
		return new SyncedBaseline(
				requirePrevious(payload, "previous_name"),
				requirePrevious(payload, "previous_description"),
				requirePrevious(payload, "previous_academic_year"));
	}

	@Transactional
	public void removeCourse(UUID courseId, UUID teacherId) {
		Course course = requireOwnedCourse(courseId, teacherId);

		if (course.isPending()) {
			Optional<ServerOperation> editableCreate = findCreateCourseOp(courseId, teacherId)
					.filter(op -> !op.isCompleted());
			if (editableCreate.isPresent()) {
				log.info("removeCourse: hard-delete pending course={} op={} teacher={}",
						courseId, editableCreate.get().getId(), teacherId);
				serverOperationService.delete(editableCreate.get().getId(), teacherId);
				return;
			}
			log.info("removeCourse: hard-delete orphan pending course={} teacher={}", courseId, teacherId);
			courseRepository.delete(course);
			return;
		}

		ServerOperation operation = saveLinkedOperation(new ServerOperation(
				ServerOperationType.DELETE_COURSE,
				coursePayload(course.getName(), course.getDescription(), course.getAcademicYear()),
				ServerOperationStatus.PENDING,
				teacherId), course.getId());
		log.info("removeCourse: enqueued DELETE_COURSE op={} course={} teacher={}",
				operation.getId(), courseId, teacherId);
	}

	@Transactional(readOnly = true)
	public CourseRequestResponse getCourseRequest(UUID operationId, UUID teacherId) {
		ServerOperation operation = serverOperationService.requireOwned(operationId, teacherId);
		assertCreateCourse(operation);
		Course course = requireLinkedCourse(operation.getId(), teacherId);
		return CourseRequestResponse.from(operation, course);
	}

	@Transactional
	public CourseRequestResponse updateCourseRequest(UUID operationId, UUID teacherId, CreateCourseRequest request) {
		ServerOperation operation = serverOperationService.requireOwned(operationId, teacherId);
		assertCreateCourse(operation);
		ServerOperationService.assertEditable(operation);

		Course course = requireLinkedCourse(operation.getId(), teacherId);
		String name = request.name().trim();
		String academicYear = request.academicYear().trim();
		String description = trimToNull(request.description());

		if (courseRepository.existsByNameAndAcademicYearAndIdNot(name, academicYear, course.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A course with this name and academic year already exists");
		}

		course.updateDetails(name, description, academicYear);
		operation.replacePayload(coursePayload(name, description, academicYear));
		operation.markPending();
		courseRepository.save(course);
		serverOperationRepository.save(operation);
		log.info("updateCourseRequest: op={} course={} teacher={}", operationId, course.getId(), teacherId);
		return CourseRequestResponse.from(operation, course);
	}

	private ServerOperation saveLinkedOperation(ServerOperation operation, UUID courseId) {
		ServerOperation saved = serverOperationRepository.save(operation);
		serverOperationCourseRepository.save(new ServerOperationCourse(saved.getId(), courseId));
		return saved;
	}

	private Course requireOwnedCourse(UUID courseId, UUID teacherId) {
		return courseRepository.findByIdAndTeacherId(courseId, teacherId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
	}

	private Course requireLinkedCourse(UUID operationId, UUID teacherId) {
		UUID courseId = serverOperationCourseRepository.findByOperationId(operationId)
				.map(ServerOperationCourse::getCourseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked course not found"));
		return requireOwnedCourse(courseId, teacherId);
	}

	private Optional<ServerOperation> findPendingCreateCourseOp(UUID courseId, UUID teacherId) {
		return findCreateCourseOp(courseId, teacherId)
				.filter(op -> op.getStatus() == ServerOperationStatus.PENDING);
	}

	private Optional<ServerOperation> findCreateCourseOp(UUID courseId, UUID teacherId) {
		return linkedOperations(courseId, teacherId).stream()
				.filter(op -> op.getType() == ServerOperationType.CREATE_COURSE)
				.findFirst();
	}

	private Optional<ServerOperation> findPendingUpdateCourseOp(UUID courseId, UUID teacherId) {
		List<ServerOperation> pendingUpdates = linkedOperations(courseId, teacherId).stream()
				.filter(op -> op.getType() == ServerOperationType.UPDATE_COURSE)
				.filter(op -> op.getStatus() == ServerOperationStatus.PENDING)
				.sorted(Comparator.comparing(ServerOperation::getCreatedAt))
				.toList();
		if (pendingUpdates.isEmpty()) {
			return Optional.empty();
		}
		for (int i = 1; i < pendingUpdates.size(); i++) {
			ServerOperation duplicate = pendingUpdates.get(i);
			serverOperationService.delete(duplicate.getId(), teacherId);
			log.info("updateCourse: dropped duplicate pending UPDATE_COURSE op={} course={} teacher={}",
					duplicate.getId(), courseId, teacherId);
		}
		return Optional.of(pendingUpdates.getFirst());
	}

	private List<ServerOperation> linkedOperations(UUID courseId, UUID teacherId) {
		return serverOperationCourseRepository.findAllByCourseId(courseId).stream()
				.map(ServerOperationCourse::getOperationId)
				.map(serverOperationRepository::findById)
				.flatMap(Optional::stream)
				.filter(op -> teacherId.equals(op.getRequestedBy()))
				.toList();
	}

	private static void assertCreateCourse(ServerOperation operation) {
		if (operation.getType() != ServerOperationType.CREATE_COURSE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a create-course operation");
		}
	}

	private static Map<String, Object> coursePayload(String name, String description, String academicYear) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", name);
		payload.put("description", description);
		payload.put("academic_year", academicYear);
		return payload;
	}

	private static Map<String, Object> changedCourseFields(String syncedName, String syncedDescription,
			String syncedAcademicYear, String name, String description, String academicYear) {
		Map<String, Object> payload = new LinkedHashMap<>();
		putIfChanged(payload, "name", syncedName, name);
		putIfChanged(payload, "description", syncedDescription, description);
		putIfChanged(payload, "academic_year", syncedAcademicYear, academicYear);
		if (!payload.isEmpty()) {
			putPrevious(payload, syncedName, syncedDescription, syncedAcademicYear);
		}
		return payload;
	}

	private static void putPrevious(Map<String, Object> payload, String name, String description,
			String academicYear) {
		payload.put("previous_name", name);
		payload.put("previous_description", description);
		payload.put("previous_academic_year", academicYear);
	}

	private static String requirePrevious(Map<String, Object> payload, String key) {
		if (!payload.containsKey(key)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Pending update is incomplete; cancel it and submit the change again");
		}
		Object value = payload.get(key);
		return value == null ? null : value.toString();
	}

	private static void putIfChanged(Map<String, Object> payload, String key, Object current, Object next) {
		if (!Objects.equals(current, next)) {
			payload.put(key, next);
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record SyncedBaseline(String name, String description, String academicYear) {
	}
}
