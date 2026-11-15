package cz.fit.tsm.faces.teacher;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.CourseRequestResponse;
import cz.fit.tsm.dto.CreateCourseRequest;
import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.faces.common.HomeBean;
import cz.fit.tsm.faces.common.Messages;
import cz.fit.tsm.service.CourseService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@Component
@RequestScope
public class TeacherCourseBean {

	private static final int ACADEMIC_YEAR_OPTIONS = 2;

	private final CourseService courseService;
	private final HomeBean homeBean;

	private String name;
	private String description;
	private String academicYear;
	private List<CourseRequestResponse> courses = Collections.emptyList();

	public TeacherCourseBean(CourseService courseService, HomeBean homeBean) {
		this.courseService = courseService;
		this.homeBean = homeBean;
	}

	@PostConstruct
	void loadCourses() {
		UserResponse user = homeBean.getCurrentUser();
		courses = user == null
				? Collections.emptyList()
				: courseService.listCourseRequests(user.id());
		List<String> years = getAcademicYearOptions();
		if (!years.isEmpty()) {
			academicYear = years.get(0);
		}
	}

	public String createCourse() {
		UserResponse user = homeBean.getCurrentUser();
		if (user == null) {
			addError(Messages.get("teacher.course.forbidden"));
			return null;
		}
		try {
			courseService.requestCourse(user.id(), new CreateCourseRequest(name, description, academicYear));
			return "/teacher/index.xhtml?faces-redirect=true";
		}
		catch (ResponseStatusException ex) {
			addError(ex.getReason() != null ? ex.getReason() : Messages.get("teacher.course.failed"));
			return null;
		}
	}

	public List<CourseRequestResponse> getCourses() {
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

	private static void addError(String detail) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_ERROR, detail, null));
	}
}
