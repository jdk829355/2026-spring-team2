# Goodz (goodsmap)

애니메이션 굿즈를 파는 매장을 지도에서 찾고, 방문 계획(플래너)을 세우는 서비스의 백엔드입니다.
AWS Cloud Clubs 경희대 Spring 스터디 팀 2 프로젝트입니다.

| 구분 | 기술 |
|---|---|
| 서버 | Java 21, Spring Boot 3.5, Spring Data JPA |
| DB | PostgreSQL (테스트는 H2 메모리 DB, PostgreSQL 모드) |
| 배포 | Docker, Amazon ECR, nginx (`deploy/`), GitHub Actions |
| 외부 연동 | Amazon S3 + CloudFront(이미지), 카카오 로컬 API(주소 → 좌표) |

## 테스트 실행

```bash
./gradlew test
```

`test` 프로필(`src/test/resources/application-test.yaml`)은 H2 메모리 DB를 쓰므로 DB나 `.env` 없이 실행됩니다.

## N+1 개선 — 상품 상세의 판매 매장 목록

상품 상세(`GET /api/v1/goods/{goodsId}`)는 그 상품을 파는 매장 목록을 함께 보여 줍니다.
`StoreGoods → Store` 연관이 지연 로딩(LAZY)이라, 처음에는 매장 목록을 조회한 뒤 매장 이름·주소를 읽을 때마다 매장 조회 쿼리가 한 번씩 더 나갔습니다.

`StoreGoodsRepository.findByGoodsId`를 `JOIN FETCH sg.store`로 바꿔 매장 정보를 한 번에 가져오게 했습니다 (`5d9359a`).

| 매장 수 | 수정 전 | 수정 후 |
|---|---|---|
| 5곳 | 쿼리 6번 | **1번** |
| 20곳 | 쿼리 21번 | **1번** |

- 측정: [`StoreGoodsNPlusOneTest`](src/test/java/team2/goodsmap/store/repository/StoreGoodsNPlusOneTest.java). 매장 N곳에 등록된 상품을 `findByGoodsId`로 조회하고 매장 이름에 접근했을 때 실행된 SQL 수를 Hibernate 통계(`getPrepareStatementCount`)로 셉니다. 영속성 컨텍스트를 비운 뒤 측정합니다.
- 수정 전 수치는 `5d9359a` 직전의 `StoreGoodsRepository`로 같은 테스트를 돌린 결과입니다.
- 쿼리 수는 DB 종류와 무관하게 JPA가 만드는 SQL로 정해지므로 H2에서 측정했습니다. 응답 시간은 측정하지 않았습니다.
- 같은 방식의 수정이 매장별 상품 목록(`findByStoreId`)과 플래너(`PlannerGoodsRepository`)에도 적용되어 있습니다.
