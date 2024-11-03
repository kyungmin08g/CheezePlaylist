package io.github.cheezeplaylist.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MusicDto {
    private String artist;
    private String title;
}
