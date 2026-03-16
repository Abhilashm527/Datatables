package com.dataflow.DataTable.repository;

import com.dataflow.DataTable.model.AiConversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiConversationRepository extends MongoRepository<AiConversation, String> {

    Optional<AiConversation> findBySessionIdAndActiveTrue(String sessionId);

    List<AiConversation> findByUserIdOrderByLastUpdatedAtDesc(String userId);

    List<AiConversation> findByApplicationIdOrderByLastUpdatedAtDesc(String applicationId);

    List<AiConversation> findByActiveTrueOrderByLastUpdatedAtDesc();
}
