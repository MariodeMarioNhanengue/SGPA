package com.sgpa.mario.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.sgpa.mario.web.domain.TokenVerificacao;
import com.sgpa.mario.web.domain.TokenVerificacao.Finalidade;
import com.sgpa.mario.web.domain.TokenVerificacao.TipoUtilizador;

import jakarta.persistence.LockModeType;

@Repository
public interface TokenVerificacaoRepository extends JpaRepository<TokenVerificacao, Long> {

    Optional<TokenVerificacao> findByTokenAndFinalidade(String token, TokenVerificacao.Finalidade finalidade);

    /**
     * Códigos de 6 dígitos ainda não consumidos de um utilizador, do mais recente para o mais antigo.
     * Com lock de escrita: dois pedidos em paralelo não podem ler o mesmo contador de tentativas
     * (senão o limite de tentativas podia ser contornado com pedidos simultâneos).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TokenVerificacao> findByTipoUtilizadorAndUtilizadorIdAndFinalidadeAndUsadoFalseAndCodigoIsNotNullOrderByDataCriacaoDesc(
        TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade);

    /** O código mais recente emitido para o utilizador, esteja ou não consumido (para o intervalo entre reenvios). */
    Optional<TokenVerificacao> findFirstByTipoUtilizadorAndUtilizadorIdAndFinalidadeAndCodigoIsNotNullOrderByDataCriacaoDesc(
        TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade);
}
