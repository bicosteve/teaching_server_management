package cz.fit.tsm.faces.teacher;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

import cz.fit.tsm.dto.ServerOperationResponse;
import cz.fit.tsm.dto.UserResponse;
import cz.fit.tsm.faces.common.HomeBean;
import cz.fit.tsm.faces.common.Messages;
import cz.fit.tsm.service.ServerOperationService;
import jakarta.inject.Named;

@Named
@Component
@SessionScope
public class TeacherRequestsBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ServerOperationService serverOperationService;
	private final HomeBean homeBean;
	private List<ServerOperationResponse> operations = List.of();

	public TeacherRequestsBean(ServerOperationService serverOperationService, HomeBean homeBean) {
		this.serverOperationService = serverOperationService;
		this.homeBean = homeBean;
		reload();
	}

	public List<ServerOperationResponse> getOperations() {
		return operations;
	}

	public void reload() {
		UserResponse user = homeBean.getCurrentUser();
		operations = user == null ? List.of() : serverOperationService.listForRequester(user.id());
	}

	public void remove(UUID operationId) {
		UserResponse user = homeBean.getCurrentUser();
		if (user == null) {
			Messages.errorKey("teacher.requests.forbidden");
			return;
		}
		try {
			serverOperationService.delete(operationId, user.id());
			reload();
		}
		catch (ResponseStatusException ex) {
			Messages.error(ex.getReason() != null ? ex.getReason() : Messages.get("teacher.requests.removeFailed"));
		}
	}
}
