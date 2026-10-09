package br.edu.fiec.FiecLab.Features.Auth.models.DTO;

import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import java.util.UUID;

public record UserMeDTO(UUID id, String name, String email, Role role) {}