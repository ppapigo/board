# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 정의가 바뀌지 않으면 이 단계와 Gradle 다운로드 캐시를 재사용한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew --no-daemon --console=plain dependencies

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew --no-daemon --console=plain bootJar -x test

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring \
    && mkdir -p /app/uploads

COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown -R spring:spring /app

USER spring
ENV APP_UPLOAD_DIR=/app/uploads
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
