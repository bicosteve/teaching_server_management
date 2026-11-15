package cz.fit.tsm.faces.common;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.security.RoleHomes;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@Component
@RequestScope
public class HomeBean {

	public UserResponse getCurrentUser() {
		Object user = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("currentUser");
		return user instanceof UserResponse response ? response : null;
	}

	public String logout() {
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
		return "/login.xhtml?faces-redirect=true";
	}

	public String homePage() {
		UserResponse user = getCurrentUser();
		return user == null ? "/login" : RoleHomes.path(user.role());
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
