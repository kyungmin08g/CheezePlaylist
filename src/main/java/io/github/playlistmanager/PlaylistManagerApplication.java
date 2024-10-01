package io.github.playlistmanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("io.github.playlistmanager.mapper")
public class PlaylistManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlaylistManagerApplication.class, args);
    }

}
