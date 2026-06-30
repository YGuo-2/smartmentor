package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.LearningPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    Page<LearningPath> findByStudentIdAndStatusOrderByCreatedAtDesc(Long studentId, String status, Pageable pageable);
    Page<LearningPath> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);
    Optional<LearningPath> findByStudentIdAndTargetKnowledgePointIdAndStatus(Long studentId, String targetKpId, String status);
    List<LearningPath> findByStudentId(Long studentId);
}
