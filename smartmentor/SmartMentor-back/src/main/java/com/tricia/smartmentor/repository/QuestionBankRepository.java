package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.QuestionBank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    Optional<QuestionBank> findByQuestionHash(String questionHash);

    long countBySourceRef(String sourceRef);

    @Query("SELECT q FROM QuestionBank q " +
            "WHERE q.knowledgePointId IN :knowledgePointIds " +
            "AND (q.qualityScore IS NULL OR q.qualityScore >= 0.60) " +
            "ORDER BY q.qualityScore DESC, q.updatedAt DESC")
    List<QuestionBank> findReusableByKnowledgePointIds(
            @Param("knowledgePointIds") Collection<String> knowledgePointIds,
            Pageable pageable);
}
