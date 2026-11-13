package br.edu.fiec.FiecLab.Features.User.service.Impl;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.Features.User.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Override
    public void setToken(String email, TokenRequestDTO dto) {
        User user = getUserByEmail(email);
        user.setFcmToken(dto.token());
        userRepository.save(user);
    }

    @Override
    public void createUser(CreateUserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setPosition(Role.CLIENTE);
        userRepository.save(user);
    }

    public void addUser(UserDTO dto) {
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setFcmToken(dto.getFcmToken());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
    }


    public User searchById(String id) {
        return userRepository.findById(id).orElseThrow();
    }

    public void deleteUserById(String id) {
        userRepository.deleteById(id);
    }

    public void UpdateUserById(String id, UserDTO dto) {
        User user = searchById(id);
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.save(user);
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