package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponseDto {
    private String artist;
    private String title;
}
