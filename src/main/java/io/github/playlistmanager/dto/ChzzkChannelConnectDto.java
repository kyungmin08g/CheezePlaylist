package io.github.playlistmanager.dto;

import lombok.*;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChzzkChannelConnectDto {
    private String playlistId;
    private String chatChannelId;
    private String accessToken;
    private String serverId;
}
