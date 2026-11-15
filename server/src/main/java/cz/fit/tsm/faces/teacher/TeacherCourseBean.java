package cz.fit.tsm.faces.teacher;

import java.io.Serializable;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.CourseRequestResponse;
import cz.fit.tsm.dto.CourseResponse;
import cz.fit.tsm.dto.CreateCourseRequest;
import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.faces.common.HomeBean;
import cz.fit.tsm.faces.common.Messages;
import cz.fit.tsm.model.ServerOperationStatus;
import cz.fit.tsm.service.CourseService;
import jakarta.inject.Named;

@Named
@Component
@SessionScope
public class TeacherCourseBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final int ACADEMIC_YEAR_OPTIONS = 2;

	private final CourseService courseService;
	private final HomeBean homeBean;
	private final TeacherNavBean nav;

	private String editId;
	private String name;
	private String description;
	private String academicYear;
	private List<CourseResponse> courses = Collections.emptyList();

	public TeacherCourseBean(CourseService courseService, HomeBean homeBean, @Lazy TeacherNavBean nav) {
		this.courseService = courseService;
		this.homeBean = homeBean;
		this.nav = nav;
		reloadCourses();
		prepareCreate();
	}

	public void reloadCourses() {
		UserResponse user = homeBean.getCurrentUser();
		courses = user == null ? List.of() : courseService.listCourses(user.id());
	}

	public void prepareCreate() {
		editId = null;
		name = null;
		description = null;
		List<String> years = getAcademicYearOptions();
		academicYear = years.isEmpty() ? null : years.get(0);
	}

	public void prepareEdit(UUID operationId) {
		UserResponse user = homeBean.getCurrentUser();
		if (user == null) {
			Messages.errorKey("teacher.course.forbidden");
			prepareCreate();
			return;
		}
		try {
			CourseRequestResponse request = courseService.getCourseRequest(operationId, user.id());
			if (request.status() == ServerOperationStatus.COMPLETED) {
				Messages.errorKey("teacher.requests.notEditable");
				prepareCreate();
				return;
			}
			editId = request.id().toString();
			name = request.name();
			description = request.description();
			academicYear = request.academicYear();
		}
		catch (ResponseStatusException ex) {
			Messages.error(ex.getReason() != null ? ex.getReason() : Messages.get("teacher.requests.notFound"));
			prepareCreate();
		}
	}

	public void saveCourse() {
		UserResponse user = homeBean.getCurrentUser();
		if (user == null) {
			Messages.errorKey("teacher.course.forbidden");
			return;
		}
		try {
			CreateCourseRequest request = new CreateCourseRequest(name, description, academicYear);
			if (!isEditMode()) {
				courseService.requestCourse(user.id(), request);
				reloadCourses();
				prepareCreate();
				nav.showCourses();
				return;
			}
			courseService.updateCourseRequest(UUID.fromString(editId), user.id(), request);
			prepareCreate();
			nav.showRequests();
		}
		catch (ResponseStatusException ex) {
			Messages.error(ex.getReason() != null ? ex.getReason() : Messages.get("teacher.course.failed"));
		}
	}

	public void cancelCourseForm() {
		boolean editing = isEditMode();
		prepareCreate();
		if (editing) {
			nav.showRequests();
		}
		else {
			nav.showCourses();
		}
	}

	public boolean isEditMode() {
		return editId != null && !editId.isBlank();
	}

	public String getEditId() {
		return editId;
	}

	public void setEditId(String editId) {
		this.editId = editId;
	}

	public List<CourseResponse> getCourses() {
		return courses;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAcademicYear() {
		return academicYear;
	}

	public void setAcademicYear(String academicYear) {
		this.academicYear = academicYear;
	}

	public List<String> getAcademicYearOptions() {
		int startYear = Year.now().getValue();
		List<String> options = new ArrayList<>(ACADEMIC_YEAR_OPTIONS);
		for (int i = 0; i < ACADEMIC_YEAR_OPTIONS; i++) {
			int year = startYear + i;
			options.add(year + "/" + (year + 1));
		}
		return options;
	}
}
