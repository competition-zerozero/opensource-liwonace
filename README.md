# opensource-liwonace

## 요구 사항

- Java 21
- Gradle Wrapper

PostgreSQL, pgvector, Ollama 및 데이터셋 연동은 이후 작업에서 구성합니다.

## 환경변수 설정

애플리케이션은 로컬 환경 설정을 환경변수로 관리합니다.

| 환경변수 | 필수 여부 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `SERVER_PORT` | 아니오 | `8080` | 애플리케이션 포트 |
| `DATASET_ROOT` | 아니오 | 빈 값 | 로컬 Company-X 데이터셋의 절대 경로 |

예시:

```bash
export DATASET_ROOT=/absolute/path/to/companyx-dataset-v1.0
```

데이터셋은 저장소 외부에 보관하며 GitHub에 커밋하지 않습니다.

## 로컬 실행

```bash
./gradlew bootRun
```

기본 포트는 `8080`이며, `SERVER_PORT` 환경변수로 변경할 수 있습니다.

## 검증

```bash
./gradlew spotlessCheck test build
```
