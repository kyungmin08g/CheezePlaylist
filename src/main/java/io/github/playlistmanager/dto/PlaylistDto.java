package io.github.playlistmanager.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistDto {
    private String id;
    private String name;
    private String chzzkId;
}
