package io.github.playlistmanager.mapper;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MusicDTO {
    private String artist;
    private String title;
}
