package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MusicFileDto {
    private String username;
    private String roomId;
    private String artist;
    private String title;
    private String musicFileBytes;
    private String donationUsername;
    private String donationPrice;
    private String donationSubscriber;
}
