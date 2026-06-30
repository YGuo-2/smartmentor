package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_session")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false, length = 100)
    private String sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(length = 200)
    private String title;

    @Column(name = "path_id")
    private Long pathId;

    @Column(name = "node_id", length = 100)
    private String nodeId;

    @Column(name = "knowledge_point_name", length = 100)
    private String knowledgePointName;

    @Column(name = "message_count")
    private Integer messageCount;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        if (messageCount == null) messageCount = 0;
        createdAt = LocalDateTime.now();
        lastActiveAt = LocalDateTime.now();
    }
}
