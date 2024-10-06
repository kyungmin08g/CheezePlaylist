package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JoinMemberDto {
    private String username;
    private String email;
    private String password;
    private String role;
}
