package br.edu.fiec.FiecLab.Features.Auth.service;

import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthLoginDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthRegisterDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthResponseDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.GoogleLoginDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.config.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, GoogleTokenVerifierService googleTokenVerifierService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
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

    public AuthResponseDTO loginWithGoogle(GoogleLoginDTO dto) {
        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(dto.idToken());

        if (payload == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do Google inválido");
        }

        User user = userRepository.findByEmail(payload.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não cadastrado"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("name", user.getName());

        return new AuthResponseDTO(jwtService.generateToken(extraClaims, user));
    }
}