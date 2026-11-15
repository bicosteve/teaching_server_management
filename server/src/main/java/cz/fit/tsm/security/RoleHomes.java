package cz.fit.tsm.security;

import cz.fit.tsm.model.UserRole;

public final class RoleHomes {

	private RoleHomes() {
	}

	public static String path(UserRole role) {
		return switch (role) {
			case ADMIN -> "/admin/index";
			case TEACHER -> "/teacher/index";
			case STUDENT -> "/student/index";
		};
	}

	public static String facesRedirect(UserRole role) {
		return path(role) + ".xhtml?faces-redirect=true";
	}
}
