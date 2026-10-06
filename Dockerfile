# ==========================================
# JAR 실행용 경량 런타임 이미지 (JAR 파일만 배포)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안용 일반 사용자 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 한국 타임존 설정
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 같은 폴더에 있는 JAR 파일을 app.jar로 복사
COPY *.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
