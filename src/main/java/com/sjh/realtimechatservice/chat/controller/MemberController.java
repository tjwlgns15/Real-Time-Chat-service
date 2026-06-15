package com.sjh.realtimechatservice.chat.controller;

import com.sjh.realtimechatservice.chat.dto.MemberRequest;
import com.sjh.realtimechatservice.chat.dto.MemberResponse;
import com.sjh.realtimechatservice.chat.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> create(@RequestBody MemberRequest request) {
        MemberResponse response = memberService.create(request);
        return ResponseEntity
                .created(URI.create("/api/members/" + response.memberId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> findAll() {
        return ResponseEntity.ok(memberService.findAll());
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> findById(@PathVariable Long memberId) {
        return ResponseEntity.ok(memberService.findById(memberId));
    }
}