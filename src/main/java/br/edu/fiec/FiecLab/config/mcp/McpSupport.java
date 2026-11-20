package br.edu.fiec.FiecLab.config.mcp;

import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilitários compartilhados pelas ferramentas MCP ({@code @McpTool}) declaradas nos controllers.
 * <p>
 * As ferramentas MCP são chamadas diretamente pelo servidor MCP (endpoint {@code /mcp}), sem passar
 * pelo pipeline do Spring MVC. Por isso, argumentos resolvidos pelo MVC ({@code @AuthenticationPrincipal},
 * {@code Pageable}, {@code MultipartFile}) são substituídos por tipos simples e convertidos aqui.
 * O {@code SecurityContext} continua disponível porque, em servlet, o servidor MCP síncrono executa
 * as ferramentas na própria thread da requisição (autenticada pelo JWT do header Authorization).
 */
public final class McpSupport {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static volatile Validator validator;

    /** Validator criado sob demanda; {@code null} se não houver provedor de Bean Validation no classpath. */
    private static Validator validator() {
        if (validator == null) {
            synchronized (McpSupport.class) {
                if (validator == null) {
                    try {
                        validator = Validation.buildDefaultValidatorFactory().getValidator();
                    } catch (RuntimeException e) {
                        return null;
                    }
                }
            }
        }
        return validator;
    }

    private McpSupport() {
    }

    /** Página serializável e estável para respostas MCP (evita serializar {@link Page} diretamente). */
    public record McpPage<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> McpPage<T> of(Page<T> page) {
            return new McpPage<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }

    /** Arquivo binário devolvido a um agente, codificado em Base64. */
    public record McpFile(String fileName, String contentType, String base64) {
    }

    /**
     * Aplica as mesmas regras de Bean Validation que o {@code @Valid} aplica nos endpoints REST,
     * já que as chamadas MCP não passam pelo Spring MVC.
     */
    public static <T> T validate(T obj) {
        Validator v = validator();
        if (obj == null || v == null) {
            return obj;
        }
        Set<ConstraintViolation<T>> violations = v.validate(obj);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        return obj;
    }

    /** Usuário autenticado na requisição MCP atual. */
    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        return user;
    }

    /** E-mail do usuário autenticado na requisição MCP atual. */
    public static String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        return auth.getName();
    }

    /** Monta um {@link Pageable} a partir de parâmetros simples, com os mesmos padrões dos endpoints REST. */
    public static Pageable pageable(Integer page, Integer size, Sort defaultSort) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(p, s, defaultSort);
    }

    /** Converte conteúdo Base64 recebido de um agente em {@link MultipartFile} para reaproveitar os services. */
    public static MultipartFile multipart(String fileName, String contentType, String base64Content) {
        if (base64Content == null || base64Content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conteúdo do arquivo (Base64) é obrigatório");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Content.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conteúdo Base64 inválido");
        }
        return new Base64MultipartFile(fileName, contentType, bytes);
    }

    private record Base64MultipartFile(String fileName, String type, byte[] bytes) implements MultipartFile {
        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return type;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
