package cz.fit.tsm.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import cz.fit.tsm.security.RoleHomes;
import cz.fit.tsm.security.UserPrincipal;

@Controller
public class HomeController {

	@GetMapping({"/", "/index", "/index.xhtml"})
	public String home(@AuthenticationPrincipal UserPrincipal principal) {
		return principal == null ? "redirect:/login" : "redirect:" + RoleHomes.path(principal.getRole());
	}

	@GetMapping({"/teacher", "/teacher/", "/admin", "/admin/", "/student", "/student/"})
	public String roleRoot(jakarta.servlet.http.HttpServletRequest request) {
		String path = request.getRequestURI().replaceAll("/$", "");
		return "redirect:" + path + "/index";
	}

	@GetMapping({
			"/teacher/courses", "/teacher/courses.xhtml",
			"/teacher/requests", "/teacher/requests.xhtml",
			"/teacher/create-course", "/teacher/create-course.xhtml"})
	public String teacherLegacyTabs() {
		return "redirect:/teacher/index";
	}
}
