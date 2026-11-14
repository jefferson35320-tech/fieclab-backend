package br.edu.fiec.FiecLab.Features.Pedidos.repository;

import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.Pedido;
import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends MongoRepository<Pedido, String> {

    Page<Pedido> findByUsuarioId(String usuarioId, Pageable pageable);

    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);
}
