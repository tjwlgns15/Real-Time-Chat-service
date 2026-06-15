package com.sjh.realtimechatservice.chat.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 메시지 읽음 수 Redis 캐시.

 * 구조:
 *   Key:   read_count:{roomId}
 *   Field: {messageId}
 *   Value: 읽은 인원 수

 * 사용 시점:
 *   - 방 입장 시: 안 읽은 메시지 messageId 목록에 대해 HINCRBY 1
 *   - 메시지 조회 시: HGET으로 읽은 수 조회 → 참여자 수 - 읽은 수 = 안 읽은 수
 *   - 그룹 퇴장 시: 해당 방 캐시 전체 삭제 (참여자 수가 바뀌므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadCountCacheManager {

    private static final String KEY_FORMAT = "read_count:%d";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 방 입장 시 안 읽은 메시지들의 읽은 인원 수를 1씩 증가.
     */
    public void incrementReadCount(Long roomId, Iterable<Long> messageIds) {
        String key = key(roomId);
        messageIds.forEach(messageId ->
                redisTemplate.opsForHash().increment(key, messageId.toString(), 1));
        log.debug("[ReadCount] 읽음 수 증가 roomId={}", roomId);
    }

    /**
     * 메시지 조회 시 읽은 인원 수 반환. 캐시 미스 시 0 반환.
     */
    public int getReadCount(Long roomId, Long messageId) {
        Object value = redisTemplate.opsForHash().get(key(roomId), messageId.toString());
        if (value == null) return 0;
        return Integer.parseInt(value.toString());
    }

    /**
     * 그룹 퇴장 시 해당 방 캐시 삭제.
     * 참여자 수가 변경되므로 기존 캐시 값이 무의미해짐.
     */
    public void deleteRoomCache(Long roomId) {
        redisTemplate.delete(key(roomId));
        log.debug("[ReadCount] 캐시 삭제 roomId={}", roomId);
    }

    private String key(Long roomId) {
        return String.format(KEY_FORMAT, roomId);
    }
}