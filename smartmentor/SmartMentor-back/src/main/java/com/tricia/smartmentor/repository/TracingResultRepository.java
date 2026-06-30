package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.TracingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TracingResultRepository extends JpaRepository<TracingResult, Long> {
    Optional<TracingResult> findByTracingId(String tracingId);
}
