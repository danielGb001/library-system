package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Member;
import com.library.librarysystem.repository.MemberRepository;
import com.library.librarysystem.service.MemberService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Member registerMember(String fullName, String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required to register a member");
        }
        Member member = new Member(fullName, email);
        return memberRepository.save(member);
    }

    @Override
    public Member getMemberById(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Member not found with id: " + id));
    }

    @Override
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    public void deleteMember(UUID id) {
        if (!memberRepository.existsById(id)) {
            throw new NoSuchElementException("Member not found with id: " + id);
        }
        memberRepository.deleteById(id);
    }
}
