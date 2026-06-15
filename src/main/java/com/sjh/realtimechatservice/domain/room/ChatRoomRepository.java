package com.sjh.realtimechatservice.domain.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 두 멤버가 공통으로 속한 DM 방 조회.
     * 그룹 채팅방 참여자 수 제한이 없으므로 type = DM 조건 필수.
     */
    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN ChatRoomMember crm1 ON crm1.room = cr AND crm1.member.id = :memberId1
            JOIN ChatRoomMember crm2 ON crm2.room = cr AND crm2.member.id = :memberId2
            WHERE cr.type = 'DM'
            """)
    Optional<ChatRoom> findDmRoom(@Param("memberId1") Long memberId1,
                                  @Param("memberId2") Long memberId2);
}