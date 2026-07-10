package team2.goodsmap.user.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnverifiedUserCleanupScheduler 단위 테스트")
class UnverifiedUserCleanupSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UnverifiedUserCleanupScheduler scheduler;

    @Test
    @DisplayName("만료된 미인증 계정 삭제 쿼리를 현재 시각 기준으로 호출한다")
    void deleteExpiredUnverifiedUsers_callsRepositoryWithCurrentTime() {
        // given
        LocalDateTime before = LocalDateTime.now();

        // when
        scheduler.deleteExpiredUnverifiedUsers();

        // then
        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).deleteByIsVerifiedFalseAndAuthCodeExpiredAtBefore(captor.capture());

        LocalDateTime passedTime = captor.getValue();
        assertThat(passedTime).isBetween(before, after);
    }
}