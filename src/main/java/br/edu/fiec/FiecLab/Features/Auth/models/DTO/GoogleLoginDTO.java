package br.edu.fiec.FiecLab.Features.Auth.models.DTO;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginDTO(
        @NotBlank(message = "O idToken é obrigatório")
        String idToken,

        // opcional: token do Firebase Cloud Messaging
        String fcmToken
) {}