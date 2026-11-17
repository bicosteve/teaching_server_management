package cz.fit.tsm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationType;

public interface ServerOperationRepository extends JpaRepository<ServerOperation, UUID> {

	List<ServerOperation> findAllByOrderByCreatedAtDesc();

	List<ServerOperation> findByRequestedByAndTypeOrderByCreatedAtDesc(UUID requestedBy, ServerOperationType type);

	List<ServerOperation> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy);

	Optional<ServerOperation> findByIdAndRequestedBy(UUID id, UUID requestedBy);
}
