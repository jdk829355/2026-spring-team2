package team2.goodsmap.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team2.goodsmap.user.enums.UserRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean isVerified = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private String authCode;

    @Column
    private LocalDateTime authCodeExpiredAt;

    @Builder
    public User(String name, String email, String password, UserRole role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isVerified = false;
    }

    public void setAuthCode(String authCode, LocalDateTime expiredAt) {
        this.authCode = authCode;
        this.authCodeExpiredAt = expiredAt;
    }

    public void verify() {
        this.isVerified = true;
        this.authCode = null;
        this.authCodeExpiredAt = null;
    }
}