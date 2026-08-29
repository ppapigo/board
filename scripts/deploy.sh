set -euo pipefail

required_envs=(
  KAKAO_REST_API
  KAKAO_SECRET
  DEPLOY_HOST
  GOOGLE_CLIENT_ID
  GOOGLE_CLIENT_SECRET
  DB_NAME
  DB_USERNAME
  DB_PASSWORD
)

missing_envs=()
for name in "${required_envs[@]}"; do
  if [ -z "${!name:-}" ]; then
    missing_envs+=("$name")
  fi
done

if [ "${#missing_envs[@]}" -gt 0 ]; then
  printf 'Missing required deployment environment variables: %s\n' "${missing_envs[*]}" >&2
  exit 1
fi

APP_PUBLIC_BASE_URL="${APP_PUBLIC_BASE_URL:-http://${DEPLOY_HOST}}"
KAKAO_CALLBACK="${KAKAO_CALLBACK:-${APP_PUBLIC_BASE_URL}/api/oauth/kakao/callback}"

JWT_SECRET_FILE="${HOME}/.board-jwt-secret"
if [ -z "${JWT_SECRET:-}" ]; then
  if [ ! -s "$JWT_SECRET_FILE" ]; then
    umask 077
    openssl rand -base64 48 > "$JWT_SECRET_FILE"
  fi
  JWT_SECRET="$(<"$JWT_SECRET_FILE")"
fi

echo ".env 생성"
cat > .env << EOF
KAKAO_REST_API=${KAKAO_REST_API}
KAKAO_SECRET=${KAKAO_SECRET}
GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
KAKAO_CALLBACK=${KAKAO_CALLBACK}
APP_PUBLIC_BASE_URL=${APP_PUBLIC_BASE_URL}
JWT_SECRET=${JWT_SECRET}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
EOF
chmod 600 .env

echo "mysql8준비 및 도커 네트워크 생성"
docker network create board-net 2>/dev/null || true
docker start mysql8 2>/dev/null || docker run -d --name mysql8 \
 --network board-net \
 -e MYSQL_ROOT_PASSWORD="${DB_PASSWORD}" \
 -e MYSQL_DATABASE="${DB_NAME}" \
 -v mysql8-data:/var/lib/mysql \
 mysql:8.0.33 --default-time-zone=+09:00
 docker network connect board-net mysql8 2>/dev/null || true

 echo "DB 응답 대기"
 timeout 60 bash -c \
 'until docker exec mysql8 mysqladmin ping -uroot -p"$DB_PASSWORD" --silent 2>/dev/null; do sleep 2; done'
 echo "mysql8 ready"

 #echo "빌드 보장을 위한 메모리 스왑"
#  if ! swapon --show | grep -q /swapfile; then
#	  sudo fallocate -l 2G /swapfile 2>/dev/null || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
#	  sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
#	  echo " swap 2G 활성화"
 # fi

 echo "GHCR 로그인"
 echo "${GHCR_TOKEN}" | docker login ghcr.io -u "${GHCR_USER}" --password-stdin


 echo "이미지 pull"
 docker compose pull

 echo "컨테이너 실행 및 헬스체크 까지 대기"
 docker compose up -d --no-build --wait
 docker logout ghcr.io


 echo "이전 이미지 정리 및 최종 상태 보고"
 docker image prune -f
 docker compose ps
