package com.library.librarysystem.controller;

import com.library.librarysystem.model.Member;
import com.library.librarysystem.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<Member> createMember(@Valid @RequestBody MemberRequest request) {
        Member created = memberService.registerMember(request.getFullName(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable UUID id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @GetMapping
    public ResponseEntity<List<Member>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable UUID id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}