package io.github.playlistmanager.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistDto {
    private String playlistId;
    private String playlistName;
    private String chzzkChannelId;
}
