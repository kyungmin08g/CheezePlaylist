FROM openjdk:24-jdk-slim

RUN apt-get update && apt-get install -y \
    ffmpeg \
    python3 \
    python3-pip \
    python3-venv
RUN python3 -m venv /app/venv
RUN /app/venv/bin/pip install yt-dlp

WORKDIR /app

COPY build/libs/*.jar /app/*.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/*.jar"]