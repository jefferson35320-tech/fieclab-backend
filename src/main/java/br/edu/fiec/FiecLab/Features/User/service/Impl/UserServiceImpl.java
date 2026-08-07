package br.edu.fiec.FiecLab.Features.User.service.Impl;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.Features.User.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    @Override
    public void setToken(TokenRequestDTO tokenRequestDTO) {
        User user = userRepository.findById(UUID.fromString("b9d353bd-24cc-48a1-84ba-b9a8b6f3e585")).orElseThrow();
        user.setFcmToken(tokenRequestDTO.token());
        userRepository.save(user);
    }
    @Override
    public void createUser(CreateUserRequestDTO createUserRequestDTO){
        User user = new User();
        user.setName(createUserRequestDTO.name());
        user.setPassword(createUserRequestDTO.password());
        user.setEmail(createUserRequestDTO.email());
        userRepository.save(user);
    }
}
