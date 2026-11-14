package br.edu.fiec.FiecLab.Features.Pedidos.controller;

import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.AtualizarStatusPedidoDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.CriarPedidoDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.PedidoDTO;
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
}
