package br.edu.fiec.FiecLab.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Chave secreta em Base64.
    // Em produção, coloque isso no application.properties/yaml
    // ou em uma variável de ambiente.
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    // Extrai o username (email) de dentro do token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extrai uma claim específica utilizando uma função de resolução
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ============================================================
    // JWT para autenticação tradicional
    // ============================================================

    // Gera o token padrão apenas com o UserDetails
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // Gera o token permitindo injetar claims personalizadas
    // (ex: roles, id, etc.)
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(
                        new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)
                )
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ============================================================
    // JWT para autenticação via OAuth2 / Google
    // ============================================================

    /**
     * Gera um JWT a partir do usuário autenticado pelo Google.
     *
     * Claims adicionadas:
     * - name: nome do usuário
     * - pictureUrl: URL da foto de perfil do Google
     *
     * O email será armazenado no "subject" (sub) do JWT.
     */
    public String generateToken(OAuth2User oauth2User) {

        Map<String, Object> claims = new HashMap<>();

        String name = oauth2User.getAttribute("name");
        String pictureUrl = oauth2User.getAttribute("picture");
        String email = oauth2User.getAttribute("email");

        claims.put("name", name);
        claims.put("pictureUrl", pictureUrl);

        return generateToken(claims, email);
    }

    /**
     * Gera um JWT utilizando um username/string como subject.
     *
     * Usado pelo fluxo OAuth2, onde o OAuth2User não implementa
     * UserDetails.
     */
    public String generateToken(
            Map<String, Object> extraClaims,
            String username
    ) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(
                        new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)
                )
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ============================================================
    // Validação
    // ============================================================

    // Valida se o token pertence ao usuário correto e se não está expirado
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {
        final String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}