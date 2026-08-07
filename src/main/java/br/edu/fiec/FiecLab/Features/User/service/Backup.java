package br.edu.fiec.FiecLab.Features.User.service;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class Backup {

    private UserRepository userRepository;

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

    public void setToken(TokenRequestDTO tokenRequestDTO) {

    }
}
