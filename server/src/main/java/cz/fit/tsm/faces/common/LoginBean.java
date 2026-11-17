package cz.fit.tsm.faces.common;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.LoginRequest;
import cz.fit.tsm.model.User;
import cz.fit.tsm.security.RoleHomes;
import cz.fit.tsm.security.UserPrincipal;
import cz.fit.tsm.service.AuthService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Named
@Component
@RequestScope
public class LoginBean {

	private static final Logger log = LoggerFactory.getLogger(LoginBean.class);

	private final AuthService authService;

	private String usernameOrEmail;
	private String password;

	public LoginBean(AuthService authService) {
		this.authService = authService;
	}

	public void redirectIfAuthenticated() throws IOException {
		UserPrincipal principal = currentPrincipal();
		if (principal == null) {
			return;
		}
		FacesContext faces = FacesContext.getCurrentInstance();
		faces.getExternalContext().redirect(
				faces.getExternalContext().getRequestContextPath() + RoleHomes.path(principal.getRole()));
		faces.responseComplete();
	}

	public String login() {
		UserPrincipal alreadyLoggedIn = currentPrincipal();
		if (alreadyLoggedIn != null) {
			return RoleHomes.facesRedirect(alreadyLoggedIn.getRole());
		}
		try {
			User user = authService.authenticate(new LoginRequest(usernameOrEmail, password));
			log.info("login: establishing session user={} role={}", user.getUsername(), user.getRole());
			UserPrincipal principal = new UserPrincipal(user);
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);

			HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
					.getExternalContext()
					.getRequest();
			HttpSession session = request.getSession(true);
			session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

			return RoleHomes.facesRedirect(user.getRole());
		}
		catch (ResponseStatusException ex) {
			log.warn("login: failed status={} reason={}", ex.getStatusCode().value(), ex.getReason());
			String detail = ex.getReason() != null ? ex.getReason() : Messages.get("login.failed");
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, detail, null));
			return null;
		}
	}

	private static UserPrincipal currentPrincipal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getPrincipal() instanceof UserPrincipal principal ? principal : null;
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
