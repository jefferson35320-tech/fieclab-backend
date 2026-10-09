package br.edu.fiec.FiecLab.Features.User.controller;

import br.edu.fiec.FiecLab.Features.Auth.models.DTO.UserMeDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Perfil", description = "Dados do usuário autenticado")
public class MeController {

    @GetMapping
    @Operation(summary = "Usuário logado", description = "Retorna os dados do dono do token (Bearer)")
    public ResponseEntity<UserMeDTO> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new UserMeDTO(user.getId(), user.getName(), user.getEmail(), user.getPosition()));
    }
}
