package io.github.playlistmanager.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class URLConversionAPI {

    // URL를 mp3로 변환하는 메소드(yt-dlp라는 파이썬 스크립트 사용함)
    public static void mp3(String youtubeURL) {
        String downloadPath = System.getProperty("user.dir") + "/src/main/resources/static/audio";

        try {
            // 프로세스 시작
            Process process = new ProcessBuilder("/opt/homebrew/bin/yt-dlp", "-x", "--audio-format", "mp3", youtubeURL)
                    .directory(new File(downloadPath)).start();

            // 명령어 출력 및 오류 메시지 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // 오류 출력
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }
            }

            // 프로세스가 완료될 때까지 대기
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("변환 완료!");
            } else {
                System.out.println("변환 실패! 오류 코드: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            e.fillInStackTrace();
        }
    }

}
