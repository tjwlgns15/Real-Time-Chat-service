package com.sjh.realtimechatservice.domain.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 특정 방의 참여자 목록 (숨김 여부 무관 — 읽음 처리, 브로드캐스트 대상 조회에 사용)
    @Query("SELECT crm FROM ChatRoomMember crm JOIN FETCH crm.member WHERE crm.room.id = :roomId")
    List<ChatRoomMember> findAllByRoomId(@Param("roomId") Long roomId);

    // 특정 멤버가 속한 방 목록 (숨김 처리된 방 제외 — 채팅방 목록 화면)
    @Query("""
            SELECT crm FROM ChatRoomMember crm
            JOIN FETCH crm.room
            WHERE crm.member.id = :memberId AND crm.hidden = false
            """)
    List<ChatRoomMember> findVisibleRoomsByMemberId(@Param("memberId") Long memberId);

    Optional<ChatRoomMember> findByRoomIdAndMemberId(Long roomId, Long memberId);

    int countByRoomId(Long roomId);
}