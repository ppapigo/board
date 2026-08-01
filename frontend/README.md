# 모아 Frontend

`docs` LLM Wiki의 현재 API 계약과 프런트엔드 요구사항을 바탕으로 만든 React + TypeScript SPA입니다.

## 실행

Node.js 20 이상이 필요합니다.

```powershell
cd frontend
npm install
npm run dev
```

개발 서버는 `http://localhost:5173`, 백엔드는 `http://localhost:8090`을 사용합니다. Vite proxy가 `/api`, `/images`, `/oauth2` 요청을 백엔드로 전달합니다.

운영처럼 별도 API origin을 사용하려면 `.env.example`을 `.env`로 복사하고 설정합니다.

```properties
VITE_API_BASE_URL=https://api.example.com
```

## 구현 범위

- 게시판 목록과 게시판별 페이지네이션
- 게시글 상세, 이미지, 작성·수정·삭제
- 댓글·답글 작성, 수정, 소프트 삭제 표시
- LIKE/DISLIKE 토글
- 이메일 로그인·회원가입, refresh cookie 기반 세션 복원
- Kakao/Google OAuth 시작 링크
- 알림 목록·읽음 처리, 내 프로필
- 관리자 게시판 CRUD
- 반응형 UI, loading/empty/error 상태

## 현재 백엔드 계약에 따른 제한

- 댓글 `children`이 실제 응답에 포함되지 않으면 답글은 표시되지 않습니다.
- 로그인 응답에 `role`이 채워지지 않으면 관리자 화면에 진입할 수 없습니다.
- OAuth 성공 후 프런트엔드로 돌아오는 callback 계약은 백엔드 보완이 필요합니다.
- 게시글 수정에서는 기존 첨부 이미지 변경을 지원하지 않습니다.

자세한 내용은 `docs/08-결정사항과-알려진-이슈.md`를 참고하세요.
