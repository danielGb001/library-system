package com.library.librarysystem.controller;

import com.library.librarysystem.model.Member;
import lombok.Getter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class MemberResponse {

    private final UUID id;
    private final String fullName;
    private final String email;
    private final LocalDate joinDate;

    public MemberResponse(UUID id, String fullName, String email, LocalDate joinDate) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.joinDate = joinDate;
    }

    // Converts a Member entity into a response so controllers never return the entity itself.
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getFullName(),
                member.getEmail(),
                member.getJoinDate()
        );
    }
}
