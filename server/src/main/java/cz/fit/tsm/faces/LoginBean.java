package cz.fit.tsm.faces;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.AuthResponse;
import cz.fit.tsm.dto.LoginRequest;
import cz.fit.tsm.service.AuthService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@Component
@RequestScope
public class LoginBean {

	private final AuthService authService;

	private String usernameOrEmail;
	private String password;

	public LoginBean(AuthService authService) {
		this.authService = authService;
	}

	public String login() {
		try {
			AuthResponse response = authService.login(new LoginRequest(usernameOrEmail, password));
			var session = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
			session.put("accessToken", response.accessToken());
			session.put("currentUser", response.user());
			return "/index.xhtml?faces-redirect=true";
		}
		catch (ResponseStatusException ex) {
			String detail = ex.getReason() != null ? ex.getReason() : Messages.get("login.failed");
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, detail, null));
			return null;
		}
	}

	public String getUsernameOrEmail() {
		return usernameOrEmail;
	}

	public void setUsernameOrEmail(String usernameOrEmail) {
		this.usernameOrEmail = usernameOrEmail;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
