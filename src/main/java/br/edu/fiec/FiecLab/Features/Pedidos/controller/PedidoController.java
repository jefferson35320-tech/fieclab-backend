package br.edu.fiec.FiecLab.Features.Pedidos.controller;

import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.AtualizarStatusPedidoDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.CriarPedidoDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.ItemPedidoRequestDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.PedidoDTO;
import br.edu.fiec.FiecLab.config.mcp.McpSupport;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import java.util.List;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.Pedido;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.StatusPedido;
import br.edu.fiec.FiecLab.Features.Pedidos.service.PedidoService;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    /** Cria o pedido com os itens do corpo ou, se omitidos, com o carrinho do usuário logado. */
    @PostMapping
    public ResponseEntity<PedidoDTO> criar(
            @AuthenticationPrincipal User usuario,
            @Valid @RequestBody(required = false) CriarPedidoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(usuario, dto));
    }

    @GetMapping("/senha/{senha}")
    public ResponseEntity<PedidoDTO> buscarPorSenha(
            @AuthenticationPrincipal User usuario,
            @PathVariable String senha) {
        return ResponseEntity.ok(service.buscarPorSenha(usuario, senha));
    }

    /** Pedidos do usuário logado, do mais recente para o mais antigo. */
    @GetMapping("/meus")
    public ResponseEntity<Page<PedidoDTO>> listarMeus(
            @AuthenticationPrincipal User usuario,
            @ParameterObject @PageableDefault(size = 10, sort = "dataPedido", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listarMeus(usuario, pageable));
    }

    /** ADMIN: todos os pedidos, com filtro opcional por status. */
    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> listarTodos(
            @AuthenticationPrincipal User usuario,
            @RequestParam(required = false) StatusPedido status,
            @ParameterObject @PageableDefault(size = 10, sort = "dataPedido", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listarTodos(usuario, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> buscar(@AuthenticationPrincipal User usuario, @PathVariable String id) {
        return ResponseEntity.ok(service.buscar(usuario, id));
    }

    /** ADMIN: avança o pedido no fluxo (PAGO, ENVIADO, ENTREGUE...). */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PedidoDTO> atualizarStatus(
            @AuthenticationPrincipal User usuario,
            @PathVariable String id,
            @Valid @RequestBody AtualizarStatusPedidoDTO dto) {
        return ResponseEntity.ok(service.atualizarStatus(usuario, id, dto.status()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoDTO> cancelar(@AuthenticationPrincipal User usuario, @PathVariable String id) {
        return ResponseEntity.ok(service.cancelar(usuario, id));
    }

    // ------------------------------------------------------------------
    // Ferramentas MCP (expostas para agentes de IA em /mcp).
    // Todas agem em nome do usuário autenticado pelo JWT da conexão MCP.
    // ------------------------------------------------------------------

    private static final Sort SORT_DATA_DESC = Sort.by(Sort.Direction.DESC, "dataPedido");

    @McpTool(name = "pedido_criar",
            description = "Cria um pedido para o usuário autenticado. Se 'itens' for omitido ou vazio, "
                    + "o pedido é criado a partir do carrinho do usuário. Nome e preço vêm sempre do banco. "
                    + "O pedido nasce com status PENDENTE e recebe uma 'senha' (código) para retirada.",
            annotations = @McpTool.McpAnnotations(title = "Criar pedido",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public PedidoDTO mcpCriar(
            @McpToolParam(description = "Itens do pedido (produtoId e quantidade >= 1). Opcional.",
                    required = false) List<ItemPedidoRequestDTO> itens) {
        CriarPedidoDTO dto = itens == null || itens.isEmpty() ? null : McpSupport.validate(new CriarPedidoDTO(itens));
        if (dto != null) {
            dto.itens().forEach(McpSupport::validate);
        }
        return service.criar(McpSupport.currentUser(), dto);
    }

    @McpTool(name = "pedido_buscar_por_senha",
            description = "ADMIN: busca um pedido pela sua senha/código de retirada (sem diferenciar maiúsculas).",
            annotations = @McpTool.McpAnnotations(title = "Buscar pedido por senha",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public PedidoDTO mcpBuscarPorSenha(
            @McpToolParam(description = "Senha/código do pedido") String senha) {
        return service.buscarPorSenha(McpSupport.currentUser(), senha);
    }

    @McpTool(name = "pedido_listar_meus",
            description = "Lista os pedidos do usuário autenticado, do mais recente para o mais antigo (paginado).",
            annotations = @McpTool.McpAnnotations(title = "Meus pedidos",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public McpSupport.McpPage<PedidoDTO> mcpListarMeus(
            @McpToolParam(description = "Número da página, começando em 0 (padrão 0)", required = false) Integer page,
            @McpToolParam(description = "Itens por página (padrão 10, máximo 100)", required = false) Integer size) {
        return McpSupport.McpPage.of(
                service.listarMeus(McpSupport.currentUser(), McpSupport.pageable(page, size, SORT_DATA_DESC)));
    }

    @McpTool(name = "pedido_listar_todos",
            description = "ADMIN: lista todos os pedidos, do mais recente para o mais antigo, com filtro opcional por status.",
            annotations = @McpTool.McpAnnotations(title = "Todos os pedidos (admin)",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public McpSupport.McpPage<PedidoDTO> mcpListarTodos(
            @McpToolParam(description = "Filtro de status: PENDENTE, PAGO, ENVIADO, ENTREGUE ou CANCELADO",
                    required = false) StatusPedido status,
            @McpToolParam(description = "Número da página, começando em 0 (padrão 0)", required = false) Integer page,
            @McpToolParam(description = "Itens por página (padrão 10, máximo 100)", required = false) Integer size) {
        return McpSupport.McpPage.of(service.listarTodos(
                McpSupport.currentUser(), status, McpSupport.pageable(page, size, SORT_DATA_DESC)));
    }

    @McpTool(name = "pedido_buscar",
            description = "Busca um pedido pelo id. Clientes só enxergam os próprios pedidos.",
            annotations = @McpTool.McpAnnotations(title = "Buscar pedido",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public PedidoDTO mcpBuscar(@McpToolParam(description = "Id do pedido") String id) {
        return service.buscar(McpSupport.currentUser(), id);
    }

    @McpTool(name = "pedido_atualizar_status",
            description = "ADMIN: avança o pedido no fluxo. Transições válidas: PENDENTE->PAGO|CANCELADO, "
                    + "PAGO->ENVIADO|CANCELADO, ENVIADO->ENTREGUE.",
            annotations = @McpTool.McpAnnotations(title = "Atualizar status do pedido (admin)",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public PedidoDTO mcpAtualizarStatus(
            @McpToolParam(description = "Id do pedido") String id,
            @McpToolParam(description = "Novo status: PAGO, ENVIADO, ENTREGUE ou CANCELADO") StatusPedido status) {
        AtualizarStatusPedidoDTO dto = McpSupport.validate(new AtualizarStatusPedidoDTO(status));
        return service.atualizarStatus(McpSupport.currentUser(), id, dto.status());
    }

    @McpTool(name = "pedido_cancelar",
            description = "Cancela um pedido. O dono só cancela pedidos PENDENTE; ADMIN também cancela pedidos PAGO.",
            annotations = @McpTool.McpAnnotations(title = "Cancelar pedido",
                    readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public PedidoDTO mcpCancelar(@McpToolParam(description = "Id do pedido") String id) {
        return service.cancelar(McpSupport.currentUser(), id);
    }
}
