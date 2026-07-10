## 작업 내용
- 스토어 관리자 API를 추가했습니다.
  - 스토어 생성
  - 내 스토어 조회
  - 스토어 관리자 추가/조회/삭제
- 요청 DTO에 입력값 검증을 넣었습니다.
- 관리자 삭제 로직의 권한 검증과 대상 검증을 보완했습니다.
- store 도메인 엔티티(Store, StoreAdmin, StoreGoods)에 auditing(createdAt)을 추가했습니다.
- repository/service/controller 테스트를 보강했습니다.

## 상세 변경
### API
- `POST /api/v1/stores`
- `GET /api/v1/stores/admin`
- `POST /api/v1/stores/{storeId}/admin`
- `GET /api/v1/stores/{storeId}/admin`
- `DELETE /api/v1/stores/{storeId}/admin/{storeAdminId}`

### 검증/권한
- `CreateStoreRequest`, `AddStoreAdminRequest`에 validation을 추가했습니다.
- 관리자 삭제 시 아래를 확인하도록 했습니다.
  - 요청자가 STORE role인지
  - 요청자가 해당 store의 관리자인지
  - 삭제 대상이 USER role 관리자인지

### Auditing
- `Store`, `StoreAdmin`, `StoreGoods`에 `createdAt`을 추가했습니다.
- `AuditingEntityListener`를 연결했습니다.
- repository 테스트로 `createdAt` 자동 세팅을 검증했습니다.

## 테스트
- `StoreControllerTest`
- `StoreServiceTest`
- `StoreRepositoryTest`

## 참고
- Notion API 명세의 관리자 삭제 항목을 구현 기준에 맞춰 반영했습니다.

<!-- HUMANIZE-SUMMARY
원본 글자수: 867
윤문본 글자수: 931
변경률: 6.2%
카테고리별 탐지 건수: A-2 3→1, A-10 4→1, I-4 4→1, H-1 1→1, D-1 0→0
자체검증: 6/6 통과
등급: B - 의미 보존은 충분하지만 변경률이 낮아 최소 개입 수준으로 마무리함
주요 변경 하이라이트:
1) 요청 DTO에 입력값 검증 추가 → 요청 DTO에 입력값 검증을 넣었습니다.
2) 관리자 삭제 권한 검증 및 대상 검증 로직 보완 → 관리자 삭제 로직의 권한 검증과 대상 검증을 보완했습니다.
3) validation 추가 → validation을 추가했습니다.
4) 자동 세팅 검증 추가 → 자동 세팅을 검증했습니다.
-->
