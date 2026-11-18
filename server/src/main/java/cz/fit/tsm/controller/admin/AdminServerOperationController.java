package cz.fit.tsm.controller.admin;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.fit.tsm.dto.OperationExecutionFeedbackRequest;
import cz.fit.tsm.dto.ServerOperationResponse;
import cz.fit.tsm.model.ServerOperationStatus;
import cz.fit.tsm.security.UserPrincipal;
import cz.fit.tsm.service.ServerOperationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/operations")
public class AdminServerOperationController {

	private static final Logger log = LoggerFactory.getLogger(AdminServerOperationController.class);

	private final ServerOperationService serverOperationService;

	public AdminServerOperationController(ServerOperationService serverOperationService) {
		this.serverOperationService = serverOperationService;
	}

	@GetMapping
	public List<ServerOperationResponse> list(@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) ServerOperationStatus status) {
		log.info("list: admin={} status={}", principal.getId(), status);
		return status == null
				? serverOperationService.listAll()
				: serverOperationService.listByStatus(status);
	}

	@GetMapping("/{operationId}")
	public ServerOperationResponse get(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID operationId) {
		log.info("get: admin={} op={}", principal.getId(), operationId);
		return serverOperationService.get(operationId);
	}

	@PostMapping(path = "/{operationId}/feedback", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ServerOperationResponse feedback(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable UUID operationId,
			@Valid @RequestBody OperationExecutionFeedbackRequest request) {
		log.info("feedback: admin={} op={} success={}", principal.getId(), operationId, request.success());
		return serverOperationService.reportExecutionFeedback(
				operationId, request.success(), request.executionResult());
	}
}
