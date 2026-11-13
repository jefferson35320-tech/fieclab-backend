package br.edu.fiec.FiecLab.Features.User.Models.Entities;

public enum Role {
    ADMIN("ROLE_ADMIN"),
    CLIENTE("ROLE_CLIENTE");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}