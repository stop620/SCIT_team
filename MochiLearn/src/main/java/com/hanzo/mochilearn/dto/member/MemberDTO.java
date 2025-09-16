package com.hanzo.mochilearn.dto.member;

import com.hanzo.mochilearn.entity.MemberEntity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {

    private Integer id;

    private String userId;
    private String password;
    private String name;
    private String nickname;

    private LocalDateTime joinDate;
    private LocalDateTime updateDate;
    private LocalDateTime lastLoginDate;

    private Role role = Role.USER;
}