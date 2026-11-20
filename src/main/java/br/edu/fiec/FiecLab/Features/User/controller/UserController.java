package br.edu.fiec.FiecLab.Features.User.controller;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.service.Impl.UserServiceImpl;
import br.edu.fiec.FiecLab.config.mcp.McpSupport;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @PostMapping("/token")
    public ResponseEntity<Void> setToken(Authentication auth, @RequestBody TokenRequestDTO dto) {
        userService.setToken(auth.getName(), dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/UserCreate")
    public ResponseEntity<Void> createUser(@RequestBody CreateUserRequestDTO createUserRequestDTO) {
        userService.createUser(createUserRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * SUPER_ADMIN: cria usuários em lote a partir de um CSV (multipart, campo "file").
     * Cabeçalho: name,email,password[,position]. Acesso restrito em SecurityConfig.
     */
    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> importUsersCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.importUsersFromCsv(file));
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    public User getCurrentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping(value = "delete/{id}", produces = APPLICATION_JSON_VALUE)
    public void deleteUser(@PathVariable String id) {
        userService.deleteUserById(id);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "find/{id}", produces = APPLICATION_JSON_VALUE)
    public User findUserById(@PathVariable String id) {
        return userService.searchById(id);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "findAll", produces = APPLICATION_JSON_VALUE)
    public List<User> findAllUsers() {
        return userService.FindAllUsers();
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping(value = "update/{id}", consumes = APPLICATION_JSON_VALUE)
    public void update(@PathVariable String id, @RequestBody UserDTO userDTO) {
        userService.UpdateUserById(id, userDTO);
    }

    // ------------------------------------------------------------------
    // Ferramentas MCP (expostas para agentes de IA em /mcp)
    // ------------------------------------------------------------------

    @McpTool(name = "usuario_definir_fcm_token",
            description = "Salva o token do Firebase Cloud Messaging (push) do usuário autenticado.",
            annotations = @McpTool.McpAnnotations(title = "Definir token FCM",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public String mcpSetToken(@McpToolParam(description = "Token FCM do dispositivo") String token) {
        userService.setToken(McpSupport.currentUserEmail(), new TokenRequestDTO(token));
        return "Token atualizado";
    }

    @McpTool(name = "usuario_criar",
            description = "Cria um usuário com perfil CLIENTE. Falha se o e-mail já estiver cadastrado.",
            annotations = @McpTool.McpAnnotations(title = "Criar usuário",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public String mcpCreateUser(
            @McpToolParam(description = "E-mail do usuário") String email,
            @McpToolParam(description = "Nome do usuário") String name,
            @McpToolParam(description = "Senha inicial") String password) {
        userService.createUser(new CreateUserRequestDTO(email, name, password));
        return "Usuário criado";
    }

    @McpTool(name = "usuario_atual",
            description = "Retorna os dados do usuário autenticado (id, nome, e-mail, perfil e carrinho).",
            annotations = @McpTool.McpAnnotations(title = "Usuário atual",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public User mcpGetCurrentUser() {
        return userService.getUserByEmail(McpSupport.currentUserEmail());
    }

    @McpTool(name = "usuario_excluir",
            description = "Exclui permanentemente um usuário pelo id. Confirme com o usuário antes de usar.",
            annotations = @McpTool.McpAnnotations(title = "Excluir usuário",
                    readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public String mcpDeleteUser(@McpToolParam(description = "Id do usuário") String id) {
        userService.deleteUserById(id);
        return "Usuário " + id + " excluído";
    }

    @McpTool(name = "usuario_buscar",
            description = "Busca um usuário pelo id.",
            annotations = @McpTool.McpAnnotations(title = "Buscar usuário",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public User mcpFindUserById(@McpToolParam(description = "Id do usuário") String id) {
        return userService.searchById(id);
    }

    @McpTool(name = "usuario_listar",
            description = "Lista todos os usuários cadastrados.",
            annotations = @McpTool.McpAnnotations(title = "Listar usuários",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<User> mcpFindAllUsers() {
        return userService.FindAllUsers();
    }

    @McpTool(name = "usuario_atualizar",
            description = "Atualiza nome e e-mail de um usuário; a senha só é alterada se informada.",
            annotations = @McpTool.McpAnnotations(title = "Atualizar usuário",
                    readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public String mcpUpdate(
            @McpToolParam(description = "Id do usuário") String id,
            @McpToolParam(description = "Novo e-mail") String email,
            @McpToolParam(description = "Novo nome") String name,
            @McpToolParam(description = "Nova senha (opcional; vazio mantém a atual)", required = false) String password) {
        userService.UpdateUserById(id, new UserDTO(email, name, password, null));
        return "Usuário " + id + " atualizado";
    }
}