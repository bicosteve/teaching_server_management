package cz.fit.tsm.faces;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import cz.fit.tsm.dto.UserResponse;
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

	public boolean isAuthenticated() {
		return getCurrentUser() != null;
	}

	public String logout() {
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
		return "/login.xhtml?faces-redirect=true";
	}

	public String displayName() {
		UserResponse user = getCurrentUser();
		if (user == null) {
			return Messages.get("app.guest");
		}
		return user.firstName() + " " + user.lastName();
	}
}
