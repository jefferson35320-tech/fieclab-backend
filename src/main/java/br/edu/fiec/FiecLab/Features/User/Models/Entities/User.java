package br.edu.fiec.FiecLab.Features.User.Models.Entities;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue
    UUID id;

    @Column(nullable = false)
    String name;


    @Column(unique = true, nullable = false)
    String email;

    @Column(nullable = false)
    String password;

    String fcmToken;

    public User(String fcmToken, UserDTO userDTO) {
        this.name = userDTO.getName();
        this.email = userDTO.getEmail();
        this.password = userDTO.getPassword();
        this.fcmToken = userDTO.getFcmToken();
    }
}
