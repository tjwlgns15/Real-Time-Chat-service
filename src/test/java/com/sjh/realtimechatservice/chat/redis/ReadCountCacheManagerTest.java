package com.sjh.realtimechatservice.chat.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Redis 컨테이너 필요 (localhost:6379)
 */
@SpringBootTest
class ReadCountCacheManagerTest {

    @Autowired
    private ReadCountCacheManager readCountCacheManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final Long ROOM_ID = 9999L;
    private static final String CACHE_KEY = "read_count:9999";

    @BeforeEach
    @AfterEach
    void cleanUp() {
        redisTemplate.delete(CACHE_KEY);
    }

    @Test
    @DisplayName("incrementReadCount 호출 시 messageId별 읽은 수가 1 증가한다")
    void incrementReadCount_정상_증가() {
        readCountCacheManager.incrementReadCount(ROOM_ID, List.of(1L, 2L, 3L));

        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 1L)).isEqualTo(1);
        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 2L)).isEqualTo(1);
        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 3L)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 메시지에 incrementReadCount를 여러 번 호출하면 누적된다")
    void incrementReadCount_누적() {
        readCountCacheManager.incrementReadCount(ROOM_ID, List.of(1L));
        readCountCacheManager.incrementReadCount(ROOM_ID, List.of(1L));
        readCountCacheManager.incrementReadCount(ROOM_ID, List.of(1L));

        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 1L)).isEqualTo(3);
    }

    @Test
    @DisplayName("캐시에 없는 messageId 조회 시 0을 반환한다")
    void getReadCount_캐시_미스_0_반환() {
        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 999L)).isEqualTo(0);
    }

    @Test
    @DisplayName("deleteRoomCache 호출 시 해당 방의 캐시가 전체 삭제된다")
    void deleteRoomCache_전체_삭제() {
        readCountCacheManager.incrementReadCount(ROOM_ID, List.of(1L, 2L, 3L));

        readCountCacheManager.deleteRoomCache(ROOM_ID);

        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 1L)).isEqualTo(0);
        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 2L)).isEqualTo(0);
        assertThat(readCountCacheManager.getReadCount(ROOM_ID, 3L)).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 방의 캐시가 독립적으로 관리된다")
    void 방별_캐시_독립() {
        Long roomId2 = 8888L;
        String cacheKey2 = "read_count:8888";

        try {
            readCountCacheManager.incrementReadCount(ROOM_ID, List.of(1L));
            readCountCacheManager.incrementReadCount(roomId2, List.of(1L));
            readCountCacheManager.incrementReadCount(roomId2, List.of(1L));

            assertThat(readCountCacheManager.getReadCount(ROOM_ID, 1L)).isEqualTo(1);
            assertThat(readCountCacheManager.getReadCount(roomId2, 1L)).isEqualTo(2);

            // roomId 캐시 삭제가 roomId2에 영향 없음
            readCountCacheManager.deleteRoomCache(ROOM_ID);
            assertThat(readCountCacheManager.getReadCount(roomId2, 1L)).isEqualTo(2);
        } finally {
            redisTemplate.delete(cacheKey2);
        }
    }
}





