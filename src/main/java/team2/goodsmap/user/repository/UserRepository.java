package team2.goodsmap.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    Optional<User> findUserByEmail(String email);

    // 이메일 + role로 조회 (개인/업체 로그인 분기)
    Optional<User> findByEmailAndRole(String email, UserRole role);

    Optional<User> findUserByIdAndRole(Long id, UserRole role);

    void deleteByIsVerifiedFalseAndAuthCodeExpiredAtBefore(LocalDateTime time);

    boolean existsByIdAndRole(Long userId, UserRole userRole);
}
