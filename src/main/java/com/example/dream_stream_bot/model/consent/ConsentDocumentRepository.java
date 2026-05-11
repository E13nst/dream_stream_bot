package com.example.dream_stream_bot.model.consent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentDocumentRepository extends JpaRepository<ConsentDocumentEntity, Long> {

    Optional<ConsentDocumentEntity> findByCodeAndCurrentTrue(ConsentCode code);

    List<ConsentDocumentEntity> findByCodeOrderByVersionDesc(ConsentCode code);

    Optional<ConsentDocumentEntity> findByCodeAndVersion(ConsentCode code, Integer version);
}
