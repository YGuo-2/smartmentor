package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUsername(String username);
    boolean existsByUsername(String username);
}
