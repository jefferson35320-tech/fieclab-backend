package br.edu.fiec.FiecLab.Features.Auth.controller;

import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthLoginDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthRegisterDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.AuthResponseDTO;
import br.edu.fiec.FiecLab.Features.Auth.models.DTO.GoogleLoginDTO;
import br.edu.fiec.FiecLab.Features.Auth.service.AuthService;
import br.edu.fiec.FiecLab.Features.Auth.service.GoogleAuthService;
import br.edu.fiec.FiecLab.config.mcp.McpSupport;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", description = "Cria um novo usuário na aplicação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já cadastrado")
    })
    public ResponseEntity<Void> register(@RequestBody @Valid AuthRegisterDTO dto) {
        authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar login", description = "Autentica o usuário e retorna o token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login efetuado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos")
    })
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthLoginDTO dto) {
        AuthResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    @Operation(summary = "Login com Google (mobile)",
            description = "Recebe o idToken obtido no app e devolve o JWT da API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login efetuado com sucesso"),
            @ApiResponse(responseCode = "401", description = "idToken inválido ou e-mail não verificado"),
            @ApiResponse(responseCode = "400", description = "idToken ausente")
    })
    public ResponseEntity<AuthResponseDTO> google(@RequestBody @Valid GoogleLoginDTO dto) {
        return ResponseEntity.ok(googleAuthService.login(dto));
    }

    // ------------------------------------------------------------------
    // Ferramentas MCP (expostas para agentes de IA em /mcp)
    // ------------------------------------------------------------------

    @McpTool(name = "auth_register",
            description = "Registra um novo usuário (perfil CLIENTE) na FiecLab. Não retorna token: "
                    + "chame auth_login em seguida para obter o JWT.",
            annotations = @McpTool.McpAnnotations(title = "Registrar usuário",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public String mcpRegister(
            @McpToolParam(description = "Nome completo do usuário") String name,
            @McpToolParam(description = "E-mail válido e ainda não cadastrado") String email,
            @McpToolParam(description = "Senha com no mínimo 6 caracteres") String password) {
        authService.register(McpSupport.validate(new AuthRegisterDTO(name, email, password)));
        return "Usuário cadastrado com sucesso";
    }

    @McpTool(name = "auth_login",
            description = "Autentica com e-mail e senha e retorna o token JWT da API "
                    + "(use-o no header Authorization: Bearer <token>).",
            annotations = @McpTool.McpAnnotations(title = "Login",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public AuthResponseDTO mcpLogin(
            @McpToolParam(description = "E-mail do usuário") String email,
            @McpToolParam(description = "Senha do usuário") String password) {
        return authService.login(McpSupport.validate(new AuthLoginDTO(email, password)));
    }

    @McpTool(name = "auth_google_login",
            description = "Troca um idToken do Google (obtido no app) pelo token JWT da API.",
            annotations = @McpTool.McpAnnotations(title = "Login com Google",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public AuthResponseDTO mcpGoogleLogin(
            @McpToolParam(description = "idToken emitido pelo Google Sign-In") String idToken,
            @McpToolParam(description = "Token do Firebase Cloud Messaging do dispositivo (opcional)",
                    required = false) String fcmToken) {
        return googleAuthService.login(McpSupport.validate(new GoogleLoginDTO(idToken, fcmToken)));
    }
}