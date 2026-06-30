package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.MasteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MasteryHistoryRepository extends JpaRepository<MasteryHistory, Long> {

    List<MasteryHistory> findByStudentIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long studentId, LocalDateTime after);

    List<MasteryHistory> findByStudentIdAndKnowledgePointIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long studentId, String knowledgePointId, LocalDateTime after);

    @Query("SELECT mh FROM MasteryHistory mh WHERE mh.studentId = :studentId " +
           "AND mh.overallMastery IS NOT NULL AND mh.createdAt >= :after " +
           "ORDER BY mh.createdAt ASC")
    List<MasteryHistory> findOverallMasteryHistory(Long studentId, LocalDateTime after);

    @Query("SELECT mh FROM MasteryHistory mh WHERE mh.studentId = :studentId " +
           "AND mh.knowledgePointId = :kpId ORDER BY mh.createdAt DESC")
    List<MasteryHistory> findLatestByStudentAndKp(Long studentId, String kpId);
}
