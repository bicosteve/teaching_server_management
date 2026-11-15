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

	@GetMapping({"/teacher", "/teacher/"})
	public String teacherHome() {
		return "redirect:/teacher/index";
	}

	@GetMapping({"/admin", "/admin/"})
	public String adminHome() {
		return "redirect:/admin/index";
	}

	@GetMapping({"/student", "/student/"})
	public String studentHome() {
		return "redirect:/student/index";
	}
}
