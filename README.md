# integration-test

7/10 회의날 draft pr 되어있던 각 도메인들 기반으로 되어있음.

## User 관련
- 업체/개인 회원가입 로직 정리 (Store 생성은 회원가입에서 제외, 로그인 후 `POST /stores`에서 처리)
- 이메일 인증 재시도 로직 (`validateAndCleanupExpiredEmail`) — 인증 대기 중 코드 만료 시 자동 삭제 후 재가입 허용
- 미인증 계정 정리 스케줄러 (`UnverifiedUserCleanupScheduler`)
- 테스트: `AuthServiceTest`, `AuthControllerTest`, `UnverifiedUserCleanupSchedulerTest` (모두 성공)

## 타 도메인 관련

### 1. `@EnableJpaAuditing` 위치 변경
- **문제**: `GoodsmapApplication`(메인 클래스)에 붙어 있어서 `@WebMvcTest` 슬라이스
  테스트(`AuthControllerTest` 등)에서 `JPA metamodel must not be empty` 에러 발생
- **조치**: `global/config/JpaAuditingConfig.java`로 분리
```java
  @Configuration
  @EnableJpaAuditing
  public class JpaAuditingConfig {
  }
```

### 2. `StoreRepositoryTest`에 `@Import` 추가
- **문제**: 위 분리 이후 `@DataJpaTest`가 일반 `@Configuration` 빈을 자동으로
  스캔하지 않아서, `createdAt` auditing이 동작하지 않아 테스트 실패
- **조치**:
```java
  @DataJpaTest
  @Import(JpaAuditingConfig.class)
  class StoreRepositoryTest {
```

### 결과
`./gradlew test` 전체 테스트 통과 (전체 성공)

## 공유 사항

- 1. **업체 승인 상태** — 아직 미구현, 관리자 승인이 필요할까?
- 2. **이메일 인증코드 재전송 API** — 아직 미구현, 이건 프론트 관련이라 혼자 해볼게욥