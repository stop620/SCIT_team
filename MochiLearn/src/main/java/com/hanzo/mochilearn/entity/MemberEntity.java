package com.hanzo.mochilearn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "member")
@EntityListeners(AuditingEntityListener.class)
public class MemberEntity {

    // 사용자 역할을 정의하는 Enum
    public enum Role {
        USER, ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Integer memberId;

    @Column(name = "user_id", nullable = false, unique = true, length = 20)
    private String userId;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    private LocalDate birth;

    @Column(length = 10)
    private String gender;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @CreatedDate // 엔티티가 처음 저장될 때 자동으로 현재 시간이 기록됩니다.
    @Column(name = "join_date", updatable = false)
    private LocalDateTime joinDate;

    @UpdateTimestamp // 엔티티가 업데이트될 때마다 자동으로 현재 시간이 기록됩니다.
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Enumerated(EnumType.STRING) // Enum의 이름을 문자열로 DB에 저장합니다.
    @Column(nullable = false, columnDefinition = "enum ('USER', 'ADMIN') default 'USER'")
    private Role role; // 기본값을 USER로 설정
}