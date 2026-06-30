package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {

    List<StudyActivity> findByStudentIdAndActivityDateOrderByCreatedAtDesc(Long studentId, LocalDate date);

    List<StudyActivity> findByStudentIdAndActivityDateBetweenOrderByCreatedAtDesc(
            Long studentId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(sa.durationMinutes), 0) FROM StudyActivity sa " +
           "WHERE sa.studentId = :studentId AND sa.activityDate = :date")
    int sumDurationByStudentAndDate(Long studentId, LocalDate date);

    @Query("SELECT COALESCE(SUM(sa.durationMinutes), 0) FROM StudyActivity sa " +
           "WHERE sa.studentId = :studentId AND sa.activityDate BETWEEN :start AND :end")
    int sumDurationByStudentAndDateBetween(Long studentId, LocalDate start, LocalDate end);

    List<StudyActivity> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT DISTINCT sa.studentId FROM StudyActivity sa " +
           "WHERE sa.activityDate BETWEEN :start AND :end")
    List<Long> findActiveStudentIdsBetween(LocalDate start, LocalDate end);

    @Query("SELECT COUNT(DISTINCT sa.activityDate) FROM StudyActivity sa " +
           "WHERE sa.studentId = :studentId AND sa.activityDate BETWEEN :start AND :end")
    int countActiveDaysByStudentBetween(Long studentId, LocalDate start, LocalDate end);
}
