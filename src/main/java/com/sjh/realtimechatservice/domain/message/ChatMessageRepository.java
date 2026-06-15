package com.sjh.realtimechatservice.domain.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 커서 기반 페이지네이션 — 특정 messageId보다 이전 메시지 조회.
     * 최초 입장 시 cursorId = null이면 최신 메시지부터 조회.
     * SYSTEM 메시지는 sender가 없으므로 LEFT JOIN FETCH 사용.
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            LEFT JOIN FETCH cm.sender
            WHERE cm.room.id = :roomId
            AND (:cursorId IS NULL OR cm.id < :cursorId)
            ORDER BY cm.id DESC
            """)
    List<ChatMessage> findByRoomIdBeforeCursor(@Param("roomId") Long roomId,
                                               @Param("cursorId") Long cursorId,
                                               Pageable pageable);

    /**
     * 방 입장 시 lastReadAt 이후의 안 읽은 메시지 일괄 조회.
     * MessageReadStatus INSERT에 사용.
     * SYSTEM 메시지는 읽음 처리 제외.
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.room.id = :roomId
            AND cm.createdAt > :lastReadAt
            AND cm.type = 'TEXT'
            AND cm.sender.id <> :memberId
            """)
    List<ChatMessage> findUnreadMessages(@Param("roomId") Long roomId,
                                         @Param("lastReadAt") LocalDateTime lastReadAt,
                                         @Param("memberId") Long memberId);
    /**
     * 채팅방 목록 — 방별 마지막 메시지 조회.
     */
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.room.id = :roomId
            ORDER BY cm.createdAt DESC
            """)
    List<ChatMessage> findLatestByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    default Optional<ChatMessage> findLastMessageByRoomId(Long roomId) {
        List<ChatMessage> result = findLatestByRoomId(roomId, Pageable.ofSize(1));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * 채팅방 목록 — lastReadAt 이후 안 읽은 메시지 수.
     */
    @Query("""
            SELECT COUNT(cm) FROM ChatMessage cm
            WHERE cm.room.id = :roomId
            AND cm.createdAt > :lastReadAt
            AND cm.type = 'TEXT'
            AND cm.sender.id <> :memberId
            """)
    int countUnreadMessages(@Param("roomId") Long roomId,
                            @Param("lastReadAt") LocalDateTime lastReadAt,
                            @Param("memberId") Long memberId);
}