package io.github.cheezeplaylist.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistDto {
    private String username;
    private String playlistId;
    private String playlistName;
    private String chzzkChannelId;
    private String donationPrice;
}
