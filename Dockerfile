# 1단계: 애플리케이션 빌드
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

# 의존성 관련 파일을 먼저 복사해 Docker 캐시 활용
RUN ./gradlew dependencies --no-daemon || true

COPY src src

RUN ./gradlew clean bootJar --no-daemon


# 2단계: 애플리케이션 실행
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]