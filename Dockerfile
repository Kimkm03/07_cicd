# 1단계: jar 만들기 (JDK + Gradle)
# alpine 키워드가 들어있는 이미지는 리눅스 자체를 경량화 시킨 베이스이미지
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
# 테스트는 이미지 만들기 전에 PC/CI 에서 먼저 돌린다. gradlew 실행 권한은 윈도우에서 빠질 수 있어 chmod 로 준다
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# 2단계: 실행만 (JRE 는 JDK 보다 훨씬 작다). 1단계에서 만든 jar 만 가져온다
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]

# docker run --rm -p 8080:8080 --env-file=.env atangi/wonbank:2stage