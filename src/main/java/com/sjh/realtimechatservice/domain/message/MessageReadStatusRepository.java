package com.sjh.realtimechatservice.domain.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

    // 이미 읽은 messageId 목록 조회 — 중복 INSERT 방지
    @Query("""
            SELECT mrs.message.id FROM MessageReadStatus mrs
            WHERE mrs.message.id IN :messageIds AND mrs.member.id = :memberId
            """)
    Set<Long> findReadMessageIds(@Param("messageIds") List<Long> messageIds,
                                 @Param("memberId") Long memberId);

    // 특정 메시지를 읽은 인원 수 조회
    int countByMessageId(Long messageId);
}