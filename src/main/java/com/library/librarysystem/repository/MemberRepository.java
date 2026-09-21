package com.library.librarysystem.repository;

import com.library.librarysystem.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
}
