package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequestDTO {
    private String artist;
    private String title;
}
