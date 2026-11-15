package cz.fit.tsm.faces.teacher;

import java.io.Serializable;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import cz.fit.tsm.model.UserRole;
import cz.fit.tsm.security.RoleHomes;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@Component
@SessionScope
public class TeacherNavBean implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum View {
		COURSES("/teacher/fragments/courses.xhtml"),
		REQUESTS("/teacher/fragments/requests.xhtml"),
		COURSE_FORM("/teacher/fragments/course-form.xhtml");

		private final String fragment;

		View(String fragment) {
			this.fragment = fragment;
		}

		String fragment() {
			return fragment;
		}
	}

	private final TeacherCourseBean courses;
	private final TeacherRequestsBean requests;
	private View view = View.COURSES;

	public TeacherNavBean(@Lazy TeacherCourseBean courses, @Lazy TeacherRequestsBean requests) {
		this.courses = courses;
		this.requests = requests;
	}

	public String getFragment() {
		return view.fragment();
	}

	public boolean isCoursesActive() {
		return view == View.COURSES || (view == View.COURSE_FORM && !courses.isEditMode());
	}

	public boolean isRequestsActive() {
		return view == View.REQUESTS || (view == View.COURSE_FORM && courses.isEditMode());
	}

	public String showCourses() {
		courses.reloadCourses();
		view = View.COURSES;
		return teacherHomeIfAway();
	}

	public String showRequests() {
		requests.reload();
		view = View.REQUESTS;
		return teacherHomeIfAway();
	}

	public void showCreateCourse() {
		courses.prepareCreate();
		view = View.COURSE_FORM;
	}

	public void showEditCourse(UUID operationId) {
		courses.prepareEdit(operationId);
		view = View.COURSE_FORM;
	}

	private String teacherHomeIfAway() {
		String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
		if (viewId != null && viewId.startsWith("/teacher/index")) {
			return null;
		}
		return RoleHomes.facesRedirect(UserRole.TEACHER);
	}
}
