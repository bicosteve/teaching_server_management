package cz.fit.tsm.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A course with this name and academic year already exists");
		}

		Course course = courseRepository.save(new Course(
				teacherId,
				name,
				description,
				academicYear,
				CourseStatus.PENDING));

		ServerOperation operation = serverOperationRepository.save(new ServerOperation(
				ServerOperationType.CREATE_COURSE,
				coursePayload(name, description, academicYear),
				ServerOperationStatus.PENDING,
				teacherId));
		serverOperationCourseRepository.save(new ServerOperationCourse(operation.getId(), course.getId()));
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

		course.updateDetails(name, description, academicYear);
		Map<String, Object> payload = coursePayload(name, description, academicYear);

		Optional<ServerOperation> pendingCreate = findPendingCreateCourseOp(courseId, teacherId);
		if (pendingCreate.isPresent()) {
			ServerOperation operation = pendingCreate.get();
			operation.replacePayload(payload);
			operation.markPending();
			serverOperationRepository.save(operation);
		}
		else {
			course.markPending();
			ServerOperation operation = serverOperationRepository.save(new ServerOperation(
					ServerOperationType.UPDATE_COURSE,
					payload,
					ServerOperationStatus.PENDING,
					teacherId));
			serverOperationCourseRepository.save(new ServerOperationCourse(operation.getId(), course.getId()));
		}

		return CourseResponse.from(courseRepository.save(course));
	}

	@Transactional
	public void removeCourse(UUID courseId, UUID teacherId) {
		Course course = requireOwnedCourse(courseId, teacherId);

		if (course.isPending()) {
			Optional<ServerOperation> editableCreate = findCreateCourseOp(courseId, teacherId)
					.filter(op -> !op.isCompleted());
			if (editableCreate.isPresent()) {
				serverOperationService.delete(editableCreate.get().getId(), teacherId);
				return;
			}
			courseRepository.delete(course);
			return;
		}

		Map<String, Object> payload = coursePayload(course.getName(), course.getDescription(), course.getAcademicYear());
		ServerOperation operation = serverOperationRepository.save(new ServerOperation(
				ServerOperationType.DELETE_COURSE,
				payload,
				ServerOperationStatus.PENDING,
				teacherId));
		serverOperationCourseRepository.save(new ServerOperationCourse(operation.getId(), course.getId()));
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
		return CourseRequestResponse.from(operation, course);
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
		return linkedOperations(courseId, teacherId).stream()
				.filter(op -> op.getType() == ServerOperationType.CREATE_COURSE)
				.filter(op -> op.getStatus() == ServerOperationStatus.PENDING)
				.findFirst();
	}

	private Optional<ServerOperation> findCreateCourseOp(UUID courseId, UUID teacherId) {
		return linkedOperations(courseId, teacherId).stream()
				.filter(op -> op.getType() == ServerOperationType.CREATE_COURSE)
				.findFirst();
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

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
