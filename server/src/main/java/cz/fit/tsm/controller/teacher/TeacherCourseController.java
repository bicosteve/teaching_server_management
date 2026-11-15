package cz.fit.tsm.controller.teacher;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.fit.tsm.dto.CourseResponse;
import cz.fit.tsm.dto.CreateCourseRequest;
import cz.fit.tsm.security.UserPrincipal;
import cz.fit.tsm.service.CourseService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teacher/courses")
public class TeacherCourseController {

	private final CourseService courseService;

	public TeacherCourseController(CourseService courseService) {
		this.courseService = courseService;
	}

	@GetMapping
	public List<CourseResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
		return courseService.listCourses(principal.getId());
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public CourseResponse create(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateCourseRequest request) {
		return courseService.requestCourse(principal.getId(), request);
	}
}
