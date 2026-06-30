package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.DiagnosticSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DiagnosticSessionRepository extends JpaRepository<DiagnosticSession, Long> {
    Optional<DiagnosticSession> findByDiagnosticId(String diagnosticId);
    Optional<DiagnosticSession> findByStudentIdAndStatus(Long studentId, String status);
    Page<DiagnosticSession> findByStudentIdOrderByStartTimeDesc(Long studentId, Pageable pageable);
    Page<DiagnosticSession> findByStudentIdAndModuleOrderByStartTimeDesc(Long studentId, String module, Pageable pageable);
}
