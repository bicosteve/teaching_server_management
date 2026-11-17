package cz.fit.tsm.faces.admin;

import java.io.Serializable;
import java.util.List;

import org.primefaces.event.SelectEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import cz.fit.tsm.dto.ServerOperationResponse;
import cz.fit.tsm.service.ServerOperationService;
import jakarta.inject.Named;

@Named
@Component
@SessionScope
public class AdminOperationsBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ServerOperationService serverOperationService;
	private List<ServerOperationResponse> operations;
	private ServerOperationResponse selected;

	public AdminOperationsBean(ServerOperationService serverOperationService) {
		this.serverOperationService = serverOperationService;
	}

	public List<ServerOperationResponse> getOperations() {
		if (operations == null) {
			operations = serverOperationService.listAll();
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
}
