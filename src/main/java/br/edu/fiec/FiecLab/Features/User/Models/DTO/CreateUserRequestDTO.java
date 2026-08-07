package br.edu.fiec.FiecLab.Features.User.Models.DTO;

public record CreateUserRequestDTO(
        String email,
        String name,
        String password
) {
}
