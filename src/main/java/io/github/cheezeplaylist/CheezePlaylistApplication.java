package io.github.cheezeplaylist;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("io.github.cheezeplaylist.mapper")
public class CheezePlaylistApplication {
    public static void main(String[] args) {
        SpringApplication.run(CheezePlaylistApplication.class, args);
    }
}
