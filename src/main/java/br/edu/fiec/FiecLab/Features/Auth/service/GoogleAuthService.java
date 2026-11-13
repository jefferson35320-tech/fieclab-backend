package br.edu.fiec.FiecLab.Features.Auth.service;

import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthResponseDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.GoogleLoginDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.config.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public GoogleAuthService(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        // Só aceita tokens emitidos para o SEU client id
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponseDTO login(GoogleLoginDTO dto) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(dto.idToken());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do Google inválido");
        }
        if (idToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do Google inválido");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail do Google não verificado");
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, name));

        if (dto.fcmToken() != null && !dto.fcmToken().isBlank()) {
            user.setFcmToken(dto.fcmToken());
            userRepository.save(user);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("name", user.getName());
        claims.put("pictureUrl", picture);

        return new AuthResponseDTO(jwtService.generateToken(claims, user));
    }

    private User createUser(String email, String name) {
        User user = new User();
        user.setName(name != null ? name : email);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPosition(Role.CLIENTE);
        return userRepository.save(user);
    }
}