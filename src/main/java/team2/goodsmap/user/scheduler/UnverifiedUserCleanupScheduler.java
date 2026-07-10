package team2.goodsmap.user.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupScheduler {

    private final UserRepository userRepository;

    // 매 1시간마다 실행 - 안전망 성격, 급하지 않음
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void deleteExpiredUnverifiedUsers() {
        userRepository.deleteByIsVerifiedFalseAndAuthCodeExpiredAtBefore(LocalDateTime.now());
    }
}