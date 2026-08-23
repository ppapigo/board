set -euo pipefail

echo ".env 생성"
cat > .env << EOF
KAKAO_REST_API= ${KAKAO_REST_API }
KAKAO_SECRET= ${KAKAO_SECRET }
GOOGLE_CLIENT_ID= ${ GOOGLE_CLIENT_ID }
GOOGLE_CLIENT_SECRET= ${GOOGLE_CLIENT_SECRET}
DB_NAME= ${DB_NAME}
DB_USERNAME= ${DB_USERNAME }
DB_PASSWORD= ${DB_PASSWORD}
EOF

echo "mysql8준비 및 도커 네트워크 생성"
docker network create board-db-net 2>/dev/null || true
docker start mysql8 2>/dev/null || docker run -d --name mysql8 \
 --network board-db-net \
 -e MYSQL_ROOT_PASSWORD=${DB_PASSWORD} \
 -e MYSQL_DATABASE=${DB_NAME} \
 -v mysql8-data:/var/lib/mysql \
 mysql:8.0.33 --default-time-zone=+09:00
 docker network connect board-db-net mysql8 2>/dev/null || true

 echo "DB 응답 대기"
 timeout 60 bash -c \
 'until docker exec mysql8 mysqladmin ping -uroot -p"$DB_PASSWORD" --silent 2>/dev/null; do sleep 2; done'
 echo "mysql8 ready"

 echo "빌드 보장을 위한 메모리 스왑"

 echo "빌드"
 docker compose build app
 docker compose build frontend

 echo "빌드 후 재기동 + 헬스체크 까지"
 docker compose up -d --wait

 echo "이전 이미지 정리 및 최종 상태 보고"
 docker image prune -f
 docker compose ps