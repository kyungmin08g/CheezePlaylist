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
    private int roomId;
    private String title;
    private byte[] musicFileBytes;
}
