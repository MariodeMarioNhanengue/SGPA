package com.example.demo.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.web.domain.Pedido;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // =========================
    // CONTAGEM POR ESTADO
    // =========================
    long countByEstado(Pedido.EstadoPedido estado);

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
    // CORREÇÃO DO ERRO AQUI
    // =========================
    List<Pedido> findByDataPedidoAfter(LocalDateTime data);

    // =========================
    // EXTRA
    // =========================
    List<Pedido> findByCompradorId(Long compradorId);

    List<Pedido> findByEstado(Pedido.EstadoPedido estado);
}