package com.sjh.realtimechatservice.chat.controller;

import com.sjh.realtimechatservice.chat.dto.ChatRoomResponse;
import com.sjh.realtimechatservice.chat.dto.ChatRoomSummaryResponse;
import com.sjh.realtimechatservice.chat.service.ChatRoomService;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // GET /api/chat/rooms?memberId=1
    @GetMapping
    public ResponseEntity<List<ChatRoomSummaryResponse>> getRooms(@RequestParam Long memberId) {
        return ResponseEntity.ok(chatRoomService.getRooms(memberId));
    }

    // POST /api/chat/rooms/dm?requesterId=1&targetId=2
    @PostMapping("/dm")
    public ResponseEntity<ChatRoomResponse> getOrCreateDmRoom(
            @RequestParam Long requesterId,
            @RequestParam Long targetId) {

        ChatRoom room = chatRoomService.getOrCreateDmRoom(requesterId, targetId);
        return ResponseEntity.ok(ChatRoomResponse.from(room));
    }

    // POST /api/chat/rooms/group
    @PostMapping("/group")
    public ResponseEntity<ChatRoomResponse> createGroupRoom(
            @RequestParam String name,
            @RequestBody List<Long> memberIds) {

        ChatRoom room = chatRoomService.createGroupRoom(name, memberIds);
        return ResponseEntity.ok(ChatRoomResponse.from(room));
    }

    // DELETE /api/chat/rooms/{roomId}/members/{memberId}
    @DeleteMapping("/{roomId}/members/{memberId}")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @PathVariable Long memberId) {

        chatRoomService.leaveRoom(roomId, memberId);
        return ResponseEntity.noContent().build();
    }
}