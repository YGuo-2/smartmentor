package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.entity.ChatSession;
import com.tricia.smartmentor.repository.AnswerRecordRepository;
import com.tricia.smartmentor.repository.ChatMessageRepository;
import com.tricia.smartmentor.repository.ChatSessionRepository;
import com.tricia.smartmentor.repository.DiagnosticSessionRepository;
import com.tricia.smartmentor.repository.LearningPathRepository;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import com.tricia.smartmentor.repository.StudentRepository;
import com.tricia.smartmentor.util.RedisUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceSecurityTest {

    private final ChatSessionRepository chatSessionRepository = mock(ChatSessionRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ContentSafetyService contentSafetyService = mock(ContentSafetyService.class);
    private final ChatService chatService = new ChatService(
            chatSessionRepository,
            chatMessageRepository,
            mock(StudentRepository.class),
            mock(StudentProfileRepository.class),
            mock(LearningPathRepository.class),
            mock(DiagnosticSessionRepository.class),
            mock(AnswerRecordRepository.class),
            mock(LlmService.class),
            mock(KnowledgeGraphService.class),
            mock(BilibiliVideoService.class),
            contentSafetyService,
            mock(ConversationalProfileService.class),
            mock(MasteryUpdateService.class),
            mock(ProfileService.class),
            mock(MemoryService.class),
            mock(RedisUtil.class),
            new ObjectMapper()
    );

    @Test
    void streamResponseRejectsForeignSessionBeforeWritingMessage() {
        ChatSession foreignSession = new ChatSession();
        foreignSession.setSessionId("s_foreign");
        foreignSession.setStudentId(200L);
        when(contentSafetyService.isInputAllowed("继续讲")).thenReturn(true);
        when(chatSessionRepository.findBySessionId("s_foreign")).thenReturn(Optional.of(foreignSession));

        ResponseStatusException ex = Assertions.assertThrows(ResponseStatusException.class,
                () -> chatService.streamResponse(100L, "继续讲", "s_foreign", null, null, null));

        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(chatMessageRepository, never()).save(any());
        verify(chatSessionRepository, never()).save(any());
    }

    @Test
    void getHistoryRejectsForeignSessionBeforeReadingMessages() {
        ChatSession foreignSession = new ChatSession();
        foreignSession.setSessionId("s_foreign");
        foreignSession.setStudentId(200L);
        when(chatSessionRepository.findBySessionId("s_foreign")).thenReturn(Optional.of(foreignSession));

        ResponseStatusException ex = Assertions.assertThrows(ResponseStatusException.class,
                () -> chatService.getHistory(100L, "s_foreign", null, 0, 10));

        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(chatMessageRepository, never()).findBySessionIdOrderByCreatedAtAsc(any());
        verify(chatMessageRepository, never()).findBySessionIdAndStudentIdOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void getHistoryRejectsMissingSessionInsteadOfReturningOrphanMessages() {
        when(chatSessionRepository.findBySessionId("s_missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = Assertions.assertThrows(ResponseStatusException.class,
                () -> chatService.getHistory(100L, "s_missing", null, 0, 10));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(chatMessageRepository, never()).findBySessionIdOrderByCreatedAtAsc(any());
        verify(chatMessageRepository, never()).findBySessionIdAndStudentIdOrderByCreatedAtAsc(any(), any());
    }
}
