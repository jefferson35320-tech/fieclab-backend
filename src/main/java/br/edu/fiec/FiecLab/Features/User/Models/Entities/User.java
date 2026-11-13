package br.edu.fiec.FiecLab.Features.User.Models.Entities;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "usuarios")
public class User implements UserDetails {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    @Indexed(unique = true, sparse = true)
    private String uuid;

    @Field("nome")
    private String name;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    @Field("fcm_token")
    private String fcmToken;

    @Field("permissao")
    private Role position = Role.CLIENTE;

    private Carrinho carrinho = new Carrinho();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(position.getAuthority()));
    }

    @Override
    public String getUsername() {
        return email;
    }
}