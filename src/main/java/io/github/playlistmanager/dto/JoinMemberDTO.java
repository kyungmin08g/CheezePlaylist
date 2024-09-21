package io.github.playlistmanager.dto;

import lombok.*;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JoinMemberDTO {
    private String username;
    private String email;
    private String password;
    private String role = "ROLE_USER";
}
