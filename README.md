# opensource-liwonace

## 요구 사항

- Java 21
- Gradle Wrapper

## 환경변수 설정

애플리케이션은 로컬 환경 설정을 환경변수로 관리합니다.

| 환경변수 | 필수 여부 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `SERVER_PORT` | 아니오 | `8080` | 애플리케이션 포트 |
| `DATASET_ROOT` | 아니오 | 빈 값 | 로컬 Company-X 데이터셋의 절대 경로 |
| `DB_HOST` | 아니오 | `localhost` | PostgreSQL 호스트 |
| `DB_PORT` | 아니오 | `5432` | PostgreSQL 포트 |
| `DB_NAME` | 아니오 | `companyx` | 데이터베이스 이름 |
| `DB_USERNAME` | 아니오 | `companyx` | 데이터베이스 사용자 |
| `DB_PASSWORD` | 아니오 | `companyx` | 데이터베이스 비밀번호 |
| `OLLAMA_BASE_URL` | 아니오 | `http://localhost:11434` | Ollama 주소 |
| `OLLAMA_EMBEDDING_MODEL` | 아니오 | `nomic-embed-text` | 임베딩 모델 |
| `OLLAMA_EMBEDDING_DIMENSION` | 아니오 | `768` | 임베딩 벡터 차원 |
| `OLLAMA_CONNECT_TIMEOUT_SECONDS` | 아니오 | `3` | Ollama 임베딩 연결 제한 시간 |
| `OLLAMA_READ_TIMEOUT_SECONDS` | 아니오 | `30` | Ollama 임베딩 응답 제한 시간 |
| `DOCUMENT_CHUNK_MAX_CHARS` | 아니오 | `2000` | 문서 chunk 최대 문자 수 |
| `DOCUMENT_DEFAULT_SEARCH_LIMIT` | 아니오 | `5` | 기본 벡터 검색 결과 수 |
| `DOCUMENT_MAX_SEARCH_LIMIT` | 아니오 | `20` | 최대 벡터 검색 결과 수 |
| `DOCUMENT_MIN_SIMILARITY` | 아니오 | `0.0` | 기본 최소 유사도 |

예시:

```bash
export DATASET_ROOT=/absolute/path/to/companyx-dataset-v1.0
```

데이터셋은 저장소 외부에 보관하며 GitHub에 커밋하지 않습니다.

## PostgreSQL 실행

Docker Compose로 PostgreSQL과 pgvector를 실행합니다.

```bash
docker compose up -d postgres
```

데이터베이스 초기화와 데이터 적재는 다음 명령으로 실행합니다.

```bash
./scripts/init-database.sh
```

적재 결과는 다음 명령으로 확인할 수 있습니다.

```bash
./scripts/verify-database.sh
```

## 문서 임베딩 적재

Ollama를 실행하고 임베딩 모델을 준비합니다.

```bash
ollama pull nomic-embed-text
```

첫 번째 터미널에서 애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

두 번째 터미널에서 문서 임베딩 적재 API를 호출합니다.

```bash
curl -X POST http://localhost:8080/internal/documents/import
```

정상 처리되면 문서 40건의 chunk가 임베딩되어 `document_chunks`에 저장됩니다.
임베딩 모델의 차원은 PostgreSQL의 `vector(768)` 설정과 일치해야 합니다.

## 검색 기능 확인

문서 벡터 검색은 다음 명령으로 확인합니다.

```bash
curl -X POST http://localhost:8080/internal/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query":"장애 보고서에서 결제 시스템 오류 원인을 찾아줘","limit":5}'
```

문서 유형과 제품명 필터를 함께 사용할 수 있습니다.

```bash
curl -X POST http://localhost:8080/internal/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query":"장애 원인","documentType":"incident_report","productName":"Product-C2"}'
```

그래프 데이터는 다음 명령으로 적재합니다.

```bash
curl -X POST http://localhost:8080/internal/graph/import
```

노드 검색은 다음 명령으로 확인합니다.

```bash
curl 'http://localhost:8080/internal/graph/nodes?type=product'
```

관계 탐색은 다음 명령으로 확인합니다.

```bash
curl -G http://localhost:8080/internal/graph/relations \
  --data-urlencode nodeId=client_1 \
  --data-urlencode depth=1 \
  --data-urlencode direction=out
```

## 로컬 실행

```bash
./gradlew bootRun
```

기본 포트는 `8080`이며, `SERVER_PORT` 환경변수로 변경할 수 있습니다.

## MCP 도구

Spring AI MCP Server를 사용해 `/mcp` 엔드포인트로 도구를 노출합니다.

| 도구 | 설명 |
| --- | --- |
| `hello` | MCP 연결 확인용 테스트 도구 |
| `vector_search` | Markdown 문서 chunk를 pgvector 유사도 기준으로 검색 |
| `nl2sql` | 자연어 질문을 안전한 `SELECT` SQL로 변환하고 PostgreSQL 조회 |
| `graph_search` | 그래프 노드와 엣지를 기준으로 Company-X 관계 데이터 탐색 |
| `generate_answer` | 도구 실행 결과를 Ollama에 전달해 한국어 자연어 답변 생성 |
| `ask_companyx` | 질문을 라우팅하고 선택된 도구 실행부터 최종 답변 생성까지 통합 처리 |

## 규칙 기반 라우팅

`ask_companyx`는 질문 키워드와 패턴을 기준으로 도구를 자동 선택합니다.

| 질문 유형 | 선택 도구 | 예시 |
| --- | --- | --- |
| 매출, 평균, 합계, 계약 수, 상위, 등록 고객 수 | `nl2sql` | `2025년 3분기 총 매출액은 얼마야?` |
| 장애, 설치 방법, 회의록, 제안서, 백업, API 인증, SSL | `vector_search` | `최근 서버 장애 사례와 원인을 알려줘` |
| 담당자, 소속 직원, 팀장, 사용 제품, 관련 프로젝트, 고객 이슈 | `graph_search` | `Client-A가 사용 중인 제품 목록은?` |

명확한 정형/관계 키워드가 없는 질문은 문서 검색인 `vector_search`를 기본값으로 사용합니다.

## 터미널 질의

웹 서버를 띄우지 않고 터미널에서 자연어 질문을 입력하려면 다음처럼 실행합니다.

```bash
DB_HOST=127.0.0.1 \
DB_PORT=15432 \
DB_NAME=companyx \
DB_USERNAME=companyx \
DB_PASSWORD=companyx \
APP_CLI_ENABLED=true \
./gradlew bootRun --args='--spring.main.web-application-type=none'
```

## 예시 질문 평가

데이터셋의 `questions.json` 30개 질문을 일괄 실행하고 평가 리포트를 생성합니다.

```bash
DB_HOST=127.0.0.1 \
DB_PORT=15432 \
DB_NAME=companyx \
DB_USERNAME=companyx \
DB_PASSWORD=companyx \
DATASET_ROOT=/absolute/path/to/companyx-dataset-v1.0 \
APP_EVALUATION_ENABLED=true \
./gradlew bootRun --args='--spring.main.web-application-type=none'
```

리포트는 기본적으로 `build/reports/companyx-evaluation.md`에 생성됩니다.

현재 로컬 평가 결과:

| 항목 | 결과 |
| --- | --- |
| 전체 질문 수 | 30 |
| 라우팅 정확도 | 100.0% |
| 실행 성공률 | 100.0% |
| 답변 생성률 | 100.0% |

평가 리포트에는 질문별 기대 도구, 선택 도구, 라우팅 성공 여부, 실행 성공 여부, 응답 시간, 최종 답변, 실패 원인이 기록됩니다.

## 한계와 개선 방향

- 규칙 기반 라우터는 예측 가능하고 빠르지만 새로운 질문 표현에는 키워드 보강이 필요합니다.
- 최종 정답률은 자동 실행 결과만으로 확정하지 않고 검색 근거와 답변 품질을 함께 수동 검토해야 합니다.
- NL2SQL은 `SELECT`만 허용하고 `LIMIT`을 강제하지만, 복잡한 질문은 추가 SQL 템플릿 또는 프롬프트 개선이 필요할 수 있습니다.
- 그래프 탐색은 제출 범위에 맞춰 최대 2단계까지만 지원합니다.
- 웹 화면, 인증, 캐시, 재시도 플러그인, LLM 기반 라우터는 3일 개발 범위에서 제외했습니다.

## 검증

```bash
./gradlew spotlessCheck test build
```
