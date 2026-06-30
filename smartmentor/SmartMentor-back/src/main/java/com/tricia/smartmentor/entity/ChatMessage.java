package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, length = 100)
    private String messageId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(length = 20)
    private String role; // student, ai

    @Column(columnDefinition = "TEXT")
    private String content;

    /** AI 检索到的学习资源（视频等）JSON 数组，按需填充 */
    @Column(columnDefinition = "TEXT")
    private String resources;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
