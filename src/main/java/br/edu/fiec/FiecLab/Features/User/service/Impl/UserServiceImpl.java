package br.edu.fiec.FiecLab.Features.User.service.Impl;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.Features.User.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Override
    public void setToken(TokenRequestDTO tokenRequestDTO) {
        // ATENÇÃO: id fixo (hardcoded). Veja a observação abaixo.
        User user = userRepository.findById("COLOQUE_AQUI_UM_ID_VALIDO").orElseThrow();
        user.setFcmToken(tokenRequestDTO.token());
        userRepository.save(user);
    }

    @Override
    public void createUser(CreateUserRequestDTO dto) {
        User user = new User();
        user.setName(dto.name());
    public void addUser(UserDTO userDTO, String token) {
        user.setPassword(passwordEncoder.encode(dto.password()));  // alterado
        user.setEmail(dto.email());
        user.setPosition(Role.USER);                                // novo
        userRepository.save(user);
    }

    public void addUser(UserDTO userDTO, String token){
        User user = new User(token, userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        userRepository.save(user);
    }

    public User searchById(String id) {
        return userRepository.findById(id).orElseThrow();
    }

    public void deleteUserById(String id) {
        userRepository.deleteById(id);
    }

    public void UpdateUserById(String id, UserDTO userDTO, String token) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setEmail(userDTO.getEmail());
            user.setName(userDTO.getName());
            user.setFcmToken(token);
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            userRepository.save(user);
        }
    }

    public List<User> FindAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));
    }
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));
    }
}
