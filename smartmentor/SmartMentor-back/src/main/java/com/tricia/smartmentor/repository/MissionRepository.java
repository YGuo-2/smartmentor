package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByStudentIdAndMissionDate(Long studentId, LocalDate date);
    Optional<Mission> findByMissionId(String missionId);
}
