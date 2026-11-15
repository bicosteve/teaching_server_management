package cz.fit.tsm.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.fit.tsm.dto.CourseRequestResponse;
import cz.fit.tsm.dto.CreateCourseRequest;
import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationStatus;
import cz.fit.tsm.model.ServerOperationType;
import cz.fit.tsm.repository.ServerOperationRepository;

@Service
public class CourseService {

	private final ServerOperationRepository serverOperationRepository;

	public CourseService(ServerOperationRepository serverOperationRepository) {
		this.serverOperationRepository = serverOperationRepository;
	}

	@Transactional
	public CourseRequestResponse requestCourse(UUID teacherId, CreateCourseRequest request) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("teacher_id", teacherId.toString());
		payload.put("name", request.name().trim());
		payload.put("description", trimToNull(request.description()));
		payload.put("academic_year", request.academicYear().trim());

		ServerOperation operation = new ServerOperation(
				ServerOperationType.CREATE_COURSE,
				payload,
				ServerOperationStatus.PENDING,
				teacherId);
		return CourseRequestResponse.from(serverOperationRepository.save(operation));
	}

	@Transactional(readOnly = true)
	public List<CourseRequestResponse> listCourseRequests(UUID teacherId) {
		return serverOperationRepository
				.findByRequestedByAndTypeOrderByCreatedAtDesc(teacherId, ServerOperationType.CREATE_COURSE)
				.stream()
				.map(CourseRequestResponse::from)
				.toList();
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
