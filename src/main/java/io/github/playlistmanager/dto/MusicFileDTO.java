package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MusicFileDTO {
    private String username;
    private String roomId;
    private String artist;
    private String title;
    private String musicFileBytes;
}
