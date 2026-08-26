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

## 검증

```bash
./gradlew spotlessCheck test build
```
