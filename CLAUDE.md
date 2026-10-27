# lotto-admin

로또 당첨번호 수집·통계·추천 번호 관리 어드민. Spring Boot 3.2 (Java 17) + React 19 (Vite).

## 구성
- 백엔드 `backend/`: 포트 `8082`, context-path `/lotto`, API `/lotto/api/v1/**`, DB MariaDB `lotto_db`
- 프론트 `frontend/`: `base: '/lotto/'`, 빌드 결과는 bootJar 시 `backend/src/main/resources/static/`으로 동기화(Sync, git 제외)
- 수집: 동행복권 API(`LottoClient`), 매주 토 21:30 KST 스케줄 → 수집 후 다음 회차 추천이 없으면 전략별 1세트씩(5세트: 독식형·트렌드형·안정형·회귀형·동반형) 자동 생성
- 추천 생성: 회차당 한 번만(이미 있으면 409). 공통 조건 = 인기 패턴 회피(AC값≥7 포함)·역대 1등 조합 제외·세트 간 공유 번호 2개 이하. 조건을 못 맞추면 조건을 풀지 않고 실패
- 추천 채점: 회차 수집 시 `target_draw`가 같은 추천의 일치 개수·등수 갱신

## 환경 변수
| 이름 | 용도 |
|---|---|
| `DB_HOST`/`DB_PORT`/`DB_NAME` | 기본 `mariadb-vm-svc:3306/lotto_db` |
| `DB_USER`/`DB_PASSWORD` | 기본값 없음 (운영은 k8s Secret `db-credentials`) |
| `LOTTO_ADMIN_TOKEN` | 쓰기(POST) API 토큰. 비어 있으면 쓰기 API 전부 401. 화면은 401 시 토큰 입력창 표시 |

## 빌드·테스트
- `cd backend && ./gradlew clean test bootJar` → `build/libs/lotto-admin-backend-0.0.1-SNAPSHOT.jar` (프론트 빌드 포함)
- 로컬 개발: 백엔드 8082 기동 후 `cd frontend && npm run dev` (프록시 `/lotto/api` → 8082)
- 운영 DB는 클러스터 내부(ClusterIP)라 로컬에서 접속 불가. 쿼리 검증은 DB 없이 컨텍스트만 띄우는 임시 테스트로 한다.

## 운영
- AKS `mzc-apps` 네임스페이스, Deployment `lotto-admin`, ingress `/lotto`, 매니페스트 `../k8s/04-lotto-admin.yaml`
- 이미지 `portalregistries.azurecr.io/lotto-admin:latest` (Dockerfile은 JAR만 복사)
- CI/CD 미구성: 배포는 수동이며 반드시 사전 승인
