package com.sgpa.mario.api.repository;

import com.sgpa.mario.web.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    Optional<Transacao> findByPedidoId(Long pedidoId);

    List<Transacao> findByEstado(Transacao.EstadoTransacao estado);
}
