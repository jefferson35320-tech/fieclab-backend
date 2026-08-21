package br.edu.fiec.FiecLab.Features.Auth.service;

import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthLoginDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthRegisterDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthResponseDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.config.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(AuthRegisterDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado!");
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());

        // Uso do PasswordEncoder no cadastro
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setPosition(Role.USER);

        userRepository.save(user);
    }

    public AuthResponseDTO login(AuthLoginDTO dto) {
        // 1. Busca o usuário pelo e-mail
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        // 2. Uso do PasswordEncoder no login para comparar a senha fornecida com o hash salvo
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        // 3. Montagem das Extra Claims que irão dentro do Payload do JWT
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("name", user.getName());

        // 4. Geração do Token usando o e-mail do usuário como Subject
        String token = jwtService.generateToken(extraClaims, user);

        return new AuthResponseDTO(token);
    }
}