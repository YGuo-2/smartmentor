package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findBySessionId(String sessionId);
    Page<ChatSession> findByStudentIdOrderByLastActiveAtDesc(Long studentId, Pageable pageable);
    Page<ChatSession> findByStudentIdAndPathIdOrderByLastActiveAtDesc(Long studentId, Long pathId, Pageable pageable);
}
