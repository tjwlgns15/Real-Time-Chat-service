package com.sjh.realtimechatservice.chat.service;

import com.sjh.realtimechatservice.chat.dto.MemberRequest;
import com.sjh.realtimechatservice.chat.dto.MemberResponse;
import com.sjh.realtimechatservice.common.exception.BusinessException;
import com.sjh.realtimechatservice.common.exception.ErrorCode;
import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberResponse create(MemberRequest request) {
        memberRepository.findByUsername(request.username()).ifPresent(m -> {
            throw new BusinessException(ErrorCode.MEMBER_DUPLICATE_USERNAME);
        });

        Member member = Member.create(request.username(), request.nickname());
        return MemberResponse.from(memberRepository.save(member));
    }

    public List<MemberResponse> findAll() {
        return memberRepository.findAll().stream()
                .map(MemberResponse::from)
                .toList();
    }

    public MemberResponse findById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }
}
