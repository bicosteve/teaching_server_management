package cz.fit.tsm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationType;

public interface ServerOperationRepository extends JpaRepository<ServerOperation, UUID> {

	List<ServerOperation> findByRequestedByAndTypeOrderByCreatedAtDesc(UUID requestedBy, ServerOperationType type);
}
