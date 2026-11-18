package cz.fit.tsm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.fit.tsm.model.ServerOperation;
import cz.fit.tsm.model.ServerOperationStatus;

public interface ServerOperationRepository extends JpaRepository<ServerOperation, UUID> {

	List<ServerOperation> findAllByOrderByCreatedAtDesc();

	List<ServerOperation> findByStatusOrderByCreatedAtDesc(ServerOperationStatus status);

	List<ServerOperation> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy);

	List<ServerOperation> findByRequestedByAndStatusOrderByCreatedAtDesc(UUID requestedBy, ServerOperationStatus status);

	Optional<ServerOperation> findByIdAndRequestedBy(UUID id, UUID requestedBy);
}
