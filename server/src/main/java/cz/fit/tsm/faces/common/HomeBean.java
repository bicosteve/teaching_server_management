package cz.fit.tsm.faces.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.faces.teacher.TeacherNavBean;
import cz.fit.tsm.model.UserRole;
import cz.fit.tsm.security.RoleHomes;
import cz.fit.tsm.security.UserPrincipal;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@Component
@RequestScope
public class HomeBean {

	private static final Logger log = LoggerFactory.getLogger(HomeBean.class);

	private final TeacherNavBean teacherNav;

	public HomeBean(@Lazy TeacherNavBean teacherNav) {
		this.teacherNav = teacherNav;
	}

	public UserResponse getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
			return UserResponse.from(principal);
		}
		return null;
	}

	public String logout() {
		log.info("logout: invalidating session");
		SecurityContextHolder.clearContext();
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
		return "/login.xhtml?faces-redirect=true";
	}

	public String goHome() {
		UserResponse user = getCurrentUser();
		if (user == null) {
			return "/login.xhtml?faces-redirect=true";
		}
		if (user.role() == UserRole.TEACHER) {
			return teacherNav.showCourses();
		}
		return RoleHomes.facesRedirect(user.role());
	}

	public String homePage() {
		UserResponse user = getCurrentUser();
		return user == null ? "/login" : RoleHomes.path(user.role());
	}

	public String getNavFragment() {
		UserResponse user = getCurrentUser();
		if (user == null) {
			return null;
		}
		return switch (user.role()) {
			case ADMIN -> "/includes/nav-admin.xhtml";
			case TEACHER -> "/includes/nav-teacher.xhtml";
			case STUDENT -> "/includes/nav-student.xhtml";
		};
	}

	public String displayName() {
		UserResponse user = getCurrentUser();
		return user == null ? Messages.get("app.guest") : user.firstName() + " " + user.lastName();
	}

	public String roleLabel() {
		UserResponse user = getCurrentUser();
		if (user == null) {
			return Messages.get("app.guest");
		}
		return Messages.get("role." + user.role().name().toLowerCase());
	}
}
