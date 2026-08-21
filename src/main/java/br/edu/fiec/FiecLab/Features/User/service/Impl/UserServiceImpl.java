package br.edu.fiec.FiecLab.Features.User.service.Impl;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.Features.User.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public void addUser(UserDTO userDTO, String token){
        userRepository.save(new User(token,userDTO));
    }

    public User searchById(UUID id){
        return userRepository.findById(id).get();
    }

    public void deleteUserById(UUID id){
        userRepository.deleteById(id);
    }

    public void UpdateUserById(UUID id,UserDTO userDTO, String token){
        User user = userRepository.findById(id).orElse(null);
        if (user != null)   {
            user.setEmail(userDTO.getEmail());
            user.setName(userDTO.getName());
            user.setFcmToken(token);
            user.setPassword(userDTO.getPassword());
            userRepository.save(user);
        }
    }

    public List<User> FindAllUsers(){
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));
    }
}
