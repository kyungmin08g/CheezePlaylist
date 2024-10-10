FROM ubuntu:22.04

# OpenJDK 설치 및 필수 패키지 설치
RUN apt-get update && apt-get install -y \
    openjdk-21-jdk \
    ffmpeg \
    python3 \
    python3-pip \
    python3-venv

# 가상 환경 생성
RUN python3 -m venv /app/venv

# 가상 환경 활성화 및 yt-dlp 설치
RUN /app/venv/bin/pip install yt-dlp

WORKDIR /app

COPY build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
