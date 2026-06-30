package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    List<AnswerRecord> findByDiagnosticIdOrderByQuestionIndexAsc(String diagnosticId);

    List<AnswerRecord> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<AnswerRecord> findByStudentIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long studentId, LocalDateTime after);

    List<AnswerRecord> findTop5ByStudentIdAndKnowledgePointIdOrderByCreatedAtDesc(
            Long studentId, String knowledgePointId);

    @Query("SELECT ar.errorType, COUNT(ar) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.isCorrect = false AND ar.errorType IS NOT NULL " +
           "GROUP BY ar.errorType ORDER BY COUNT(ar) DESC")
    List<Object[]> countErrorsByType(Long studentId);

    @Query("SELECT ar.errorType, ar.errorDetail, COUNT(ar) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.isCorrect = false AND ar.errorType IS NOT NULL " +
           "GROUP BY ar.errorType, ar.errorDetail ORDER BY COUNT(ar) DESC")
    List<Object[]> countErrorsByTypeAndDetail(Long studentId);

    @Query("SELECT COUNT(ar) FROM AnswerRecord ar WHERE ar.studentId = :studentId")
    long countByStudentId(Long studentId);

    @Query("SELECT COUNT(ar) FROM AnswerRecord ar WHERE ar.studentId = :studentId AND ar.isCorrect = true")
    long countCorrectByStudentId(Long studentId);

    @Query("SELECT COUNT(ar) FROM AnswerRecord ar WHERE ar.studentId = :studentId AND ar.isCorrect = false")
    long countIncorrectByStudentId(Long studentId);

    @Query("SELECT COUNT(ar) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.createdAt >= :after")
    long countByStudentIdAfter(Long studentId, LocalDateTime after);

    @Query("SELECT COUNT(ar) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.isCorrect = true AND ar.createdAt >= :after")
    long countCorrectByStudentIdAfter(Long studentId, LocalDateTime after);

    @Query("SELECT COUNT(ar) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.isCorrect = false AND ar.createdAt >= :after")
    long countIncorrectByStudentIdAfter(Long studentId, LocalDateTime after);

    @Query("SELECT COALESCE(AVG(ar.timeSpent), 0) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.timeSpent IS NOT NULL")
    double avgTimeSpentByStudentId(Long studentId);

    @Query("SELECT ar.errorType, COUNT(ar) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.isCorrect = false " +
           "AND ar.errorType IS NOT NULL AND ar.createdAt >= :after " +
           "GROUP BY ar.errorType ORDER BY COUNT(ar) DESC")
    List<Object[]> countErrorsByTypeAfter(Long studentId, LocalDateTime after);

    @Query("SELECT ar.errorType, COUNT(ar) FROM AnswerRecord ar " +
           "WHERE ar.studentId = :studentId AND ar.isCorrect = false " +
           "AND ar.errorType IS NOT NULL AND ar.createdAt < :before " +
           "GROUP BY ar.errorType ORDER BY COUNT(ar) DESC")
    List<Object[]> countErrorsByTypeBefore(Long studentId, LocalDateTime before);
}
