package com.hanzo.mochilearn.dto;

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

    Integer memberId;
    String userId;
    String password;
    String name;
    LocalDate birth;
    String gender;
    String email;
    String phone;
    String nickname;
    LocalDateTime joinDate;
    LocalDateTime updateDate;
    LocalDateTime lastLoginDate;
    String role;

}