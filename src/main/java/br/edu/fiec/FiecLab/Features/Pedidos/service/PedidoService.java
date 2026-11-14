package br.edu.fiec.FiecLab.Features.Pedidos.service;

import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.CriarPedidoDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.ItemPedidoDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.ItemPedidoRequestDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO.PedidoDTO;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.ItemPedido;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.Pedido;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.StatusPedido;
import br.edu.fiec.FiecLab.Features.Pedidos.repository.PedidoRepository;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import br.edu.fiec.FiecLab.Features.Produts.Repositories.ProductRepository;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.ItemCarrinho;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductRepository productRepository,
                         MongoTemplate mongoTemplate) {
        this.pedidoRepository = pedidoRepository;
        this.productRepository = productRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // ------------------------------------------------------------------
    // Criação
    // ------------------------------------------------------------------

    public PedidoDTO criar(User usuario, CriarPedidoDTO dto) {
        boolean doCarrinho = dto == null || dto.itens() == null || dto.itens().isEmpty();

        // produtoId -> quantidade (soma quantidades se o mesmo produto vier repetido)
        Map<String, Integer> solicitados = new LinkedHashMap<>();
        if (doCarrinho) {
            if (usuario.getCarrinho() != null) {
                for (ItemCarrinho item : usuario.getCarrinho().getItens()) {
                    solicitados.merge(item.getProdutoId(), item.getQuantidade(), Integer::sum);
                }
            }
        } else {
            for (ItemPedidoRequestDTO item : dto.itens()) {
                solicitados.merge(item.produtoId(), item.quantidade(), Integer::sum);
            }
        }

        if (solicitados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe os itens do pedido ou preencha o carrinho antes de finalizar.");
        }
        if (solicitados.values().stream().anyMatch(q -> q <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade de cada item deve ser maior que zero.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        Pedido salvo;
        try {
            for (Map.Entry<String, Integer> entrada : solicitados.entrySet()) {
                Product produto = baixarEstoque(entrada.getKey(), entrada.getValue());
                // nome e preço vêm do banco, nunca do cliente
                itens.add(new ItemPedido(produto.getId(), produto.getName(), entrada.getValue(), produto.getPrice()));
            }

            Pedido pedido = new Pedido();
            pedido.setUsuarioId(usuario.getId());
            pedido.setDataPedido(LocalDateTime.now());
            pedido.setStatus(StatusPedido.PENDENTE);
            pedido.setItens(itens);
            pedido.setValorTotal(calcularTotal(itens));
            salvo = pedidoRepository.save(pedido);
        } catch (RuntimeException e) {
            devolverEstoque(itens);   // desfaz o que já foi baixado
            throw e;
        }

        if (doCarrinho) {
            limparCarrinho(usuario.getId());
        }
        return toDTO(salvo);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Page<PedidoDTO> listarMeus(User usuario, Pageable pageable) {
        return pedidoRepository.findByUsuarioId(usuario.getId(), pageable).map(this::toDTO);
    }

    public Page<PedidoDTO> listarTodos(User usuario, StatusPedido status, Pageable pageable) {
        exigirAdmin(usuario);
        Page<Pedido> pagina = status == null
                ? pedidoRepository.findAll(pageable)
                : pedidoRepository.findByStatus(status, pageable);
        return pagina.map(this::toDTO);
    }

    public PedidoDTO buscar(User usuario, String id) {
        Pedido pedido = obter(id);
        verificarAcesso(usuario, pedido);
        return toDTO(pedido);
    }

    // ------------------------------------------------------------------
    // Mudança de status
    // ------------------------------------------------------------------

    /** Somente ADMIN. */
    public PedidoDTO atualizarStatus(User usuario, String id, StatusPedido novoStatus) {
        exigirAdmin(usuario);
        Pedido pedido = obter(id);
        if (!pedido.getStatus().podeIrPara(novoStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transição inválida: " + pedido.getStatus() + " -> " + novoStatus);
        }
        return toDTO(transicionar(pedido, novoStatus));
    }

    /** O dono cancela enquanto estiver PENDENTE; ADMIN cancela também pedidos PAGO. */
    public PedidoDTO cancelar(User usuario, String id) {
        Pedido pedido = obter(id);
        verificarAcesso(usuario, pedido);

        if (!ehAdmin(usuario) && pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Só é possível cancelar pedidos pendentes.");
        }
        if (!pedido.getStatus().podeIrPara(StatusPedido.CANCELADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pedido " + pedido.getStatus() + " não pode ser cancelado.");
        }
        return toDTO(transicionar(pedido, StatusPedido.CANCELADO));
    }

    /**
     * Troca o status só se ele ainda for o que lemos (evita duas requisições
     * simultâneas cancelarem o mesmo pedido e devolverem o estoque duas vezes).
     */
    private Pedido transicionar(Pedido atual, StatusPedido novo) {
        Query query = new Query(Criteria.where("id").is(atual.getId()).and("status").is(atual.getStatus()));
        Pedido atualizado = mongoTemplate.findAndModify(
                query,
                new Update().set("status", novo),
                FindAndModifyOptions.options().returnNew(true),
                Pedido.class);

        if (atualizado == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O pedido foi alterado por outra requisição. Tente novamente.");
        }
        if (novo == StatusPedido.CANCELADO) {
            devolverEstoque(atualizado.getItens());
        }
        return atualizado;
    }

    // ------------------------------------------------------------------
    // Estoque
    // ------------------------------------------------------------------

    private Product baixarEstoque(String produtoId, int quantidade) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("id").is(produtoId),
                Criteria.where("ativo").is(true),
                Criteria.where("estoque.quantidade").gte(quantidade)));

        Update update = new Update()
                .inc("estoque.quantidade", -quantidade)
                .set("estoque.dataAtualizacao", LocalDateTime.now());

        Product produto = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Product.class);

        if (produto == null) {
            if (!productRepository.existsById(produtoId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado: " + produtoId);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Produto inativo ou sem estoque suficiente: " + produtoId);
        }
        return produto;
    }

    private void devolverEstoque(List<ItemPedido> itens) {
        for (ItemPedido item : itens) {
            try {
                mongoTemplate.updateFirst(
                        new Query(Criteria.where("id").is(item.getProdutoId())),
                        new Update()
                                .inc("estoque.quantidade", item.getQuantidade())
                                .set("estoque.dataAtualizacao", LocalDateTime.now()),
                        Product.class);
            } catch (RuntimeException e) {
                // não mascara o erro original; fica registrado para correção manual
                log.error("Falha ao devolver estoque do produto {} (qtd {})", item.getProdutoId(), item.getQuantidade(), e);
            }
        }
    }

    private void limparCarrinho(String usuarioId) {
        mongoTemplate.updateFirst(
                new Query(Criteria.where("id").is(usuarioId)),
                new Update().set("carrinho.itens", new ArrayList<>()),
                User.class);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private Pedido obter(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado: " + id));
    }

    private boolean ehAdmin(User usuario) {
        return usuario.getPosition() == Role.ADMIN;
    }

    private void exigirAdmin(User usuario) {
        if (!ehAdmin(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a administradores.");
        }
    }

    /** Cliente só enxerga os próprios pedidos; devolve 404 (e não 403) para não revelar que o id existe. */
    private void verificarAcesso(User usuario, Pedido pedido) {
        if (!ehAdmin(usuario) && !pedido.getUsuarioId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado: " + pedido.getId());
        }
    }

    private BigDecimal calcularTotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PedidoDTO toDTO(Pedido p) {
        List<ItemPedidoDTO> itens = p.getItens().stream()
                .map(i -> new ItemPedidoDTO(i.getProdutoId(), i.getNome(), i.getQuantidade(), i.getPrecoUnitario()))
                .toList();
        return new PedidoDTO(p.getId(), p.getUsuarioId(), p.getDataPedido(), p.getValorTotal(), p.getStatus(), itens);
    }
}
