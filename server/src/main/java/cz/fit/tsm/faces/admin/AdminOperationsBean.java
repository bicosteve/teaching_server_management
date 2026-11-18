package cz.fit.tsm.faces.admin;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.primefaces.event.SelectEvent;
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
public class AdminOperationsBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ServerOperationService serverOperationService;
	private final HomeBean homeBean;
	private List<ServerOperationResponse> operations;
	private ServerOperationResponse selected;

	public AdminOperationsBean(ServerOperationService serverOperationService, HomeBean homeBean) {
		this.serverOperationService = serverOperationService;
		this.homeBean = homeBean;
	}

	public List<ServerOperationResponse> getOperations() {
		if (operations == null) {
			reload();
		}
		return operations;
	}

	public ServerOperationResponse getSelected() {
		return selected;
	}

	public void setSelected(ServerOperationResponse selected) {
		this.selected = selected;
	}

	public void onRowSelect(SelectEvent<ServerOperationResponse> event) {
		selected = serverOperationService.get(event.getObject().id());
	}

	public void approve(UUID operationId) {
		UserResponse user = homeBean.getCurrentUser();
		if (user == null) {
			Messages.errorKey("admin.operations.forbidden");
			return;
		}
		try {
			serverOperationService.approve(operationId, user.id());
			reload();
			Messages.infoKey("admin.operations.approveSuccess");
		}
		catch (ResponseStatusException ex) {
			Messages.error(ex.getReason() != null ? ex.getReason() : Messages.get("admin.operations.approveFailed"));
		}
	}

	public void reject(UUID operationId) {
		UserResponse user = homeBean.getCurrentUser();
		if (user == null) {
			Messages.errorKey("admin.operations.forbidden");
			return;
		}
		try {
			serverOperationService.reject(operationId, user.id());
			reload();
			Messages.infoKey("admin.operations.rejectSuccess");
		}
		catch (ResponseStatusException ex) {
			Messages.error(ex.getReason() != null ? ex.getReason() : Messages.get("admin.operations.rejectFailed"));
		}
	}

	private void reload() {
		operations = serverOperationService.listAll();
		if (selected != null) {
			selected = operations.stream()
					.filter(op -> op.id().equals(selected.id()))
					.findFirst()
					.orElse(null);
		}
	}
}
