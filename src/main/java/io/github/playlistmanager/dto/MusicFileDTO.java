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
    private String roomId;
    private String artist;
    private String title;
    private byte[] musicFileBytes;
}
