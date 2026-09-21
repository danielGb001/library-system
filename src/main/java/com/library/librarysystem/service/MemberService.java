package com.library.librarysystem.service;

import com.library.librarysystem.model.Member;
import java.util.List;
import java.util.UUID;

public interface MemberService {
    Member registerMember(String fullName, String email);
    Member getMemberById(UUID id);
    List<Member> getAllMembers();
    void deleteMember(UUID id);
}
