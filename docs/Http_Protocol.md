# Http Protocol

1. 주요 메서드
   - GET: 조회
   - POST: 생성, 처리
   - PUT: 수정
   - DELETE: 삭제
   - PATCH: 부분 수정
   - OPTION: 옵션

1. 상태 코드
   - 200: OK, 성공
   - 201: Created, 생성됨
   - 400: Bad Request, 요청이 잘못됨
   - 401: Unauthorized, 인증필요함
   - 403: Forbidden, 인증은 됐지만 권한이 없음
   - 404: Not Found, 요청하는 리소스가 존재하지 않음
   - 409: Conflict, 충돌(중복)
   - 500: Internal Server Error, 내부 서버 에러


1. Authentication(인증) vs Authorization(인가)
   - 인증: 사용자를 식별
   - 인가: 권한을 판별하는것

# docker 명령어
# 도커 파일 실행(컨테이너화)
-d: 백그라운드 실행
-p: 포트포워딩
-name: 컨테이너이름

제일 뒤에 이미지 이름
docker run -d -p 8090:8090 --name board-back-end board


#컨테이너 중지
docker stop board-back-end

#컨테이너 삭제
docker rm board-back-end

#컨테이너 목록
docker ps
docker ps -a 중지된 컨테이너 목록도 보여줌

#실행중인 컨테이너 로그 보기
# -f:로그 추적(실시간), ctrl+c 누르면 빠져나옴
docker logs -f board-back-end