package br.edu.fiec.FiecLab.Features.User.Models.Entities;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Users")
public class User implements UserDetails {

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

    Role position;

    public User(String fcmToken, UserDTO userDTO) {
        this.name = userDTO.getName();
        this.email = userDTO.getEmail();
        this.password = userDTO.getPassword();
        this.fcmToken = userDTO.getFcmToken();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(position.getAuthority()));
    }

    @Override
    public String getUsername() {
        return email;
    }
}
