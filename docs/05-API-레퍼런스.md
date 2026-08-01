---
title: API 레퍼런스
aliases: [API, API Contract]
tags: [board, api, contract]
status: current
last-reviewed: 2026-08-01
---

# API 레퍼런스

기준 base URL은 `http://localhost:8090`이다. 🔒는 access token 필수, `ADMIN`은 관리자 권한, `AUTHOR`는 작성자 권한을 뜻한다.

## 공통 형식

보호 요청 헤더:

```http
Authorization: Bearer <JWT>
```

오류 응답:

```json
{
  "code": "POST_NOT_FOUND",
  "message": "서버에 정의된 한국어 메시지",
  "timestamp": "2026-08-01T12:00:00"
}
```

페이지 요청은 `?page=0&size=10&sort=createdAt,desc` 형태다. 컨트롤러 기본값은 API별 표를 참고한다.

## 인증

| Method | Path | 입력 | 성공 |
|---|---|---|---|
| POST | `/api/auth/signup` | `SignupRequest` | 201 `IngestResult` |
| POST | `/api/auth/login` | `LoginRequest` | 200 `UserResponse` + refresh cookie |
| POST | `/api/auth/logout` | refresh cookie 선택 | 200 empty + expired cookie |
| POST | `/api/auth/reissue` | refresh cookie 필수 | 200 `TokenResponse` |

```ts
interface SignupRequest { email: string; password: string; nick_name: string; role: string }
interface LoginRequest { email: string; password: string }
interface UserResponse { id: number; email: string; nickName: string; accessToken: string; refreshToken: string; role: string | null }
interface TokenResponse { accessToken: string; refreshToken: string }
interface IngestResult { status: string; message: string | null }
```

검증: 로그인 password 최소 8자, 회원가입 password 8~30자, 모든 필드 non-blank. 현재 회원가입 `role`은 `"ADMIN"`일 때 관리자, 그 외 사용자로 저장된다.

## OAuth

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/oauth/kakao/login` | state cookie 설정 후 Kakao로 302 |
| GET | `/api/oauth/kakao/callback?code=&state=` | 직접 Kakao callback, `UserResponse` |
| GET | `/oauth2/authorization/kakao` | Spring OAuth2 Kakao 시작 |
| GET | `/oauth2/authorization/google` | Spring OIDC Google 시작 |

## 게시판

| Method | Path | 권한 | 입력/성공 |
|---|---|---|---|
| GET | `/api/board/all` | 공개 | `BoardResponse[]` |
| POST | `/api/board/new` | ADMIN | `BoardRequest` → `BoardResponse` |
| PUT | `/api/board/{id}/update` | ADMIN | `BoardRequest` → `BoardResponse` |
| DELETE | `/api/board/{id}` | ADMIN | `"ok"` |

```ts
interface BoardRequest { name: string; description?: string | null }
interface BoardResponse { id: number; name: string; description: string | null; createdAt: string }
```

`name`은 생성 시 필수다. 수정 controller에는 현재 `@Valid`가 없어 검증 동작이 생성과 다를 수 있다.

## 게시글

| Method | Path | 권한 | 입력/성공 |
|---|---|---|---|
| GET | `/api/post/all` | 공개 | `PostDTO[]` |
| GET | `/api/post/{boardId}/all` | 공개 | `Page<PostDTO>`, 기본 10개·최신순 |
| GET | `/api/post/{id}` | 공개 | `PostDTO` |
| POST | `/api/post/{boardId}/new` | 🔒 | multipart → `PostDTO` |
| PUT | `/api/post/{id}/update` | AUTHOR | `PostRequest` JSON → `PostDTO` |
| DELETE | `/api/post/{id}` | AUTHOR | `"ok"` |

```ts
interface PostRequest { title: string; body: string }
interface PostImageResponse { id: number; url: string; originalName: string | null; sortOrder: number }
interface PostDTO {
  id: number; title: string; author: string; board: string; body: string | null;
  viewCount: number; images: PostImageResponse[];
  like: number | null; dislike: number | null; myReaction: 'LIKE' | 'DISLIKE' | null;
  createdAt: string; canEdit: boolean; canDelete: boolean;
}
```

생성 multipart 예시:

```js
const form = new FormData();
form.append('post', new Blob([JSON.stringify({ title, body })], { type: 'application/json' }));
images.forEach((image) => form.append('images', image));
```

`Content-Type` 헤더는 브라우저가 boundary와 함께 설정하게 둔다. 이미지는 최대 5개다. 이미지 URL은 상대 경로이므로 API origin과 결합한다.

> [!warning]
> 반응 집계와 `canEdit/canDelete`는 상세 조회에서만 신뢰한다. 목록/생성/수정 응답은 현재 이 필드를 채우지 않거나 기본값을 반환한다.

## 댓글

| Method | Path | 권한 | 입력/성공 |
|---|---|---|---|
| GET | `/api/comment/post/{postId}/list` | 공개 | `Page<CommentResponse>`, 기본 10개·루트 최신순 |
| POST | `/api/comment/posts/{postId}/new` | 🔒 | `CommentCreateRequest` → `CommentResponse` |
| PUT | `/api/comment/{id}` | AUTHOR | `CommentCreateRequest` → `CommentResponse` |
| DELETE | `/api/comment/{id}` | AUTHOR | 200 empty, 소프트 삭제 |

```ts
interface CommentCreateRequest { parentId: number | null; content: string }
interface CommentResponse {
  id: number; authorUserName: string; content: string;
  parent: number | null; createdAt: string; deleted: boolean;
}
```

내용은 최대 100자다. 소스는 자식 댓글을 포함하려는 흔적이 있지만 현재 DTO에 `children` 필드가 없어 실제 중첩 응답 계약이 불완전하다. 프런트 구현 전에 확정해야 한다.

## 게시글 반응

| Method | Path | 권한 | 입력/성공 |
|---|---|---|---|
| POST | `/api/post/{postId}/reaction` | 🔒 | `{ "type": "LIKE" }` → `ReactionResponse` |

```ts
interface ReactionResponse { likeCount: number; dislikeCount: number; myReaction: 'LIKE' | 'DISLIKE' | null }
```

같은 반응을 다시 보내면 취소되고, 다른 반응을 보내면 변경된다.

## 댓글 반응

| Method | Path | 권한 | 입력/성공 |
|---|---|---|---|
| POST | `/api/comment/{commentId}/reaction` | 🔒 | `{ "type": "LIKE" }` → `ReactionResponse` |

게시글 반응과 동일하게 같은 반응은 취소되고 다른 반응은 변경된다. 존재하지 않는 댓글은 `COMMENT_NOT_FOUND`를 반환한다.

## 알림

| Method | Path | 권한 | 입력/성공 |
|---|---|---|---|
| GET | `/api/notify/list` | 🔒 | `Page<NotificationResponse>`, 기본 10개·오래된 순 |
| GET | `/api/notify/unreads` | 🔒 | `{ "unreadCount": number }` |
| PUT | `/api/notify/{id}/read` | 🔒 | 200 empty |

```ts
interface NotificationResponse {
  id: number; type: 'COMMENT_ON_POST' | 'REPLY_ON_COMMENT'; message: string;
  actorUsername: string; postId: number; commentId: number;
  read: boolean; createdAt: string;
}
```

## 사용자

| Method | Path | 권한 | 성공 |
|---|---|---|---|
| GET | `/api/user/me` | 🔒 | `UserProfileResponse` |

```ts
interface UserProfileResponse { nickName: string; phoneNumber: string | null; birth: string; createdAt: string }
```

## 주요 오류 코드

`USER_NOT_FOUND`, `BOARD_NOT_FOUND`, `POST_NOT_FOUND`, `COMMENT_NOT_FOUND`, `DUPLICATE_USER_EMAIL`, `DUPLICATE_BOARD_NAME`, `ACCESS_DENIED`, `POST_ACCESS_DENIED`, `BOARD_ACCESS_DENIED`, `LOGIN_REQUIRED`, `LOGIN_FAILED`, `INVALID_INPUT`, `INVALID_OAUTH_STATE`, `INVALID_FILE_TYPE`, `FILE_COUNT_EXCEEDED`, `MAX_UPLOAD_SIZE_EXCEEDED`, `CANNOT_REPLY_TO_REPLY`, `CANNOT_REPLY_TO_DELETED`, `CANNOT_EDIT_DELETED`, `COMMENT_POST_MISMATCH`, `CANNOT_VIEW_NOTIFICATIOIN`, `METHOD_NOT_ALLOWED`, `SQL_INTEGRITY_ERROR`, `INTERNAL_SERVER_ERROR`.
