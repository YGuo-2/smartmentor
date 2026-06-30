package com.tricia.smartmentor.repository;

import com.tricia.smartmentor.entity.StudentMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentMemoryRepository extends JpaRepository<StudentMemory, Long> {

    /** 召回：取该生全部记忆，在内存里算相似度。学生记忆量级百级，全量拉取可接受。 */
    List<StudentMemory> findByStudentId(Long studentId);

    /** 写入去重：同一学生同一内容（content_hash）已存在则跳过。 */
    boolean existsByStudentIdAndContentHash(Long studentId, String contentHash);
}
