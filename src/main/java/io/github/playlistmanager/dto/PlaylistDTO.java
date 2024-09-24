package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistDTO {
    private int roomId;
    private String playlistName;
}
