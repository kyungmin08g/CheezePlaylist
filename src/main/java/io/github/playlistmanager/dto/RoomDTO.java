package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDTO {
    private int roomId;
    private String roomName;
}
