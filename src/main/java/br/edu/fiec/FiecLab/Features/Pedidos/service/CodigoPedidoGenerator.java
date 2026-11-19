package br.edu.fiec.FiecLab.Features.Pedidos.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodigoPedidoGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TAMANHO = 12;
    private final SecureRandom random = new SecureRandom();

    public String gerar() {
        StringBuilder sb = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}