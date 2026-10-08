package com.sgpa.mario.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgpa.mario.web.domain.Pedido;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // =========================
    // CONTAGEM POR ESTADO
    // =========================
    long countByEstado(Pedido.EstadoPedido estado);

    // ✅ Atalhos úteis (opcional mas bom)
    default long countPendentes() {
        return countByEstado(Pedido.EstadoPedido.PENDENTE);
    }

    default long countAceites() {
        return countByEstado(Pedido.EstadoPedido.ACEITE);
    }

    default long countRejeitados() {
        return countByEstado(Pedido.EstadoPedido.REJEITADO);
    }

    // =========================
    // MAIS RECENTES
    // =========================
    List<Pedido> findTop3ByOrderByDataPedidoDesc();

    // =========================
    // FILTROS POR DATA
    // =========================
    List<Pedido> findByDataPedidoAfter(LocalDateTime data);

    List<Pedido> findByDataPedidoBetween(LocalDateTime inicio, LocalDateTime fim);

    // =========================
    // FILTROS POR USUÁRIO
    // =========================
    List<Pedido> findByCompradorId(Long compradorId);

    // 🔥 IMPORTANTE (para futuro login do produtor)
    List<Pedido> findByComprador_Id(Long compradorId);

    // =========================
    // FILTROS POR ESTADO
    // =========================
    List<Pedido> findByEstado(Pedido.EstadoPedido estado);

    // =========================
    // EXTRA (ÚTIL NO DASHBOARD)
    // =========================
    List<Pedido> findTop5ByEstadoOrderByDataPedidoDesc(Pedido.EstadoPedido estado);

    // =========================
    // PEDIDOS POR PRODUTOR
    // =========================
    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM Pedido p WHERE p.oferta.produtor.id = :produtorId ORDER BY p.dataPedido DESC")
    List<Pedido> findByProdutorId(@org.springframework.data.repository.query.Param("produtorId") Long produtorId);
}