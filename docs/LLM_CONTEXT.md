---
title: LLM 작업 컨텍스트
aliases: [AI Context, Codex Context]
tags: [board, llm, instructions]
status: current
last-reviewed: 2026-08-01
---

# LLM 작업 컨텍스트

## 프로젝트 정체성

Java 21/Spring Boot/MySQL 기반 커뮤니티 REST 백엔드다. 프런트엔드는 아직 없으며 [[07-프런트엔드-요구사항]]은 제안 요구사항이다.

## 새 작업을 시작할 때 읽는 순서

1. [[README]]
2. 작업 영역에 따라 [[05-API-레퍼런스]], [[03-도메인-모델]], [[04-인증과-권한]]
3. 프런트 관련이면 [[07-프런트엔드-요구사항]]
4. 반드시 [[08-결정사항과-알려진-이슈]]

## 사실 우선순위

1. 현재 소스 코드와 실행 가능한 테스트
2. `status: current` 문서
3. `status: proposed` 요구사항
4. 기존 [[Http_Protocol]] 메모

문서와 코드가 충돌하면 조용히 추측하지 말고 차이를 보고한다. 목표 동작과 현재 동작을 구분한다.

## 구현 원칙

- Controller annotation에서 실제 method/path/권한을 확인한다.
- DTO와 validation annotation을 API schema의 근거로 삼는다.
- 공개 GET과 인증 쓰기라는 SecurityConfig 정책을 보존한다.
- 작성자 전용 변경은 `@PreAuthorize` 검사를 우회하지 않는다.
- refresh token은 HttpOnly cookie, access token은 Authorization header라는 방향을 유지한다.
- 댓글은 루트 + 한 단계 답글이며 삭제는 soft delete다.
- 이미지 생성 제한은 최대 5개, 파일당 2MB, 전체 20MB다.
- 사용자 소유의 기존 변경사항을 덮어쓰지 않는다.

## API 변경 체크리스트

- [[05-API-레퍼런스]]의 endpoint와 TypeScript shape 갱신
- [[07-프런트엔드-요구사항]]에 미치는 영향 확인
- 인증/권한이면 [[04-인증과-권한]] 갱신
- 해결한 이슈는 [[08-결정사항과-알려진-이슈]]에서 상태 기록
- 정상, validation, 401, 403, 404와 핵심 비즈니스 규칙 테스트

## LLM이 가정하면 안 되는 것

- `UserResponse.role`이 항상 존재한다는 가정
- 목록의 `like`, `dislike`, `canEdit`, `canDelete`가 정확하다는 가정
- 댓글 응답에 children이 온다는 가정
- OAuth가 프런트 화면으로 redirect된다는 가정
- 모든 access token 문자열에 `Bearer `가 이미 붙었다는 가정
- 상대 이미지 URL이 프런트 origin에 있다는 가정

## 주요 소스 위치

```text
src/main/java/com/sbs/board/*/*Controller.java  API 진입점
src/main/java/com/sbs/board/*/*Service.java     비즈니스 규칙
src/main/java/com/sbs/board/*/dto/              요청/응답
src/main/java/com/sbs/board/global/entity/      영속 모델
src/main/java/com/sbs/board/global/config/      보안/MVC
src/main/resources/application.yaml             런타임 설정
```

