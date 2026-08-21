package br.edu.fiec.FiecLab.Features.User.service;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public interface UserService extends UserDetailsService {
    void setToken(TokenRequestDTO tokenRequestDTO);

    void createUser(CreateUserRequestDTO createUserRequestDTO);
}
