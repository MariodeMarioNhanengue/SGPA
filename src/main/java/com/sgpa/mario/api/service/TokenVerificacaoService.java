package com.sgpa.mario.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgpa.mario.web.domain.TokenVerificacao;
import com.sgpa.mario.web.domain.TokenVerificacao.*;
import com.sgpa.mario.api.repository.TokenVerificacaoRepository;

/**
 * Geração e validação de tokens de uso único (recuperação de senha)
 * e de códigos de 6 dígitos (confirmação de email). Não sabe nada sobre email ou sobre as
 * entidades Produtor/Comprador/Admin em si — apenas gere o ciclo de vida do token/código.
 */
@Service
public class TokenVerificacaoService {

    /** Tentativas erradas permitidas por código; ao esgotar-se é preciso pedir um código novo. */
    public static final int MAX_TENTATIVAS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern SEIS_DIGITOS = Pattern.compile("\\d{6}");

    public enum EstadoCodigo {
        /** Código certo; fica consumido. */
        CONFIRMADO,
        /** Código errado (ainda restam tentativas). */
        INCORRETO,
        /** Não há código pendente, ou o pendente já expirou. */
        EXPIRADO,
        /** As tentativas esgotaram-se: o código foi anulado. */
        BLOQUEADO,
        /** O texto escrito não são 6 dígitos; não gasta tentativas. */
        FORMATO_INVALIDO
    }

    public record ResultadoCodigo(EstadoCodigo estado, int tentativasRestantes) {
        public static ResultadoCodigo de(EstadoCodigo estado) { return new ResultadoCodigo(estado, 0); }
    }

    private final TokenVerificacaoRepository tokenRepository;

    public TokenVerificacaoService(TokenVerificacaoRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String gerarToken(TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade, Duration validade) {
        TokenVerificacao t = novo(tipo, utilizadorId, finalidade, validade);
        tokenRepository.save(t);
        return t.getToken();
    }

    /** Devolve o token apenas se existir, ainda não tiver sido usado, e não tiver expirado. */
    public Optional<TokenVerificacao> validar(String token, Finalidade finalidade) {
        return tokenRepository.findByTokenAndFinalidade(token, finalidade)
            .filter(TokenVerificacao::isValido);
    }

    public void marcarUsado(TokenVerificacao token) {
        token.setUsado(true);
        tokenRepository.save(token);
    }

    // ─── Códigos de 6 dígitos (confirmação de e-mail) ────────────────────

    /**
     * Gera um código novo de 6 dígitos para o utilizador e anula os anteriores ainda pendentes
     * (só o último e-mail enviado vale).
     */
    @Transactional
    public String gerarCodigo(TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade, Duration validade) {
        List<TokenVerificacao> anteriores = tokenRepository
            .findByTipoUtilizadorAndUtilizadorIdAndFinalidadeAndUsadoFalseAndCodigoIsNotNullOrderByDataCriacaoDesc(
                tipo, utilizadorId, finalidade);
        anteriores.forEach(a -> a.setUsado(true));
        tokenRepository.saveAll(anteriores);

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        TokenVerificacao t = novo(tipo, utilizadorId, finalidade, validade);
        t.setCodigo(codigo);
        tokenRepository.save(t);
        return codigo;
    }

    /** true se já foi emitido um código a este utilizador há menos de {@code intervalo} (anti-spam de reenvios). */
    public boolean codigoEmitidoHaMenosDe(TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade, Duration intervalo) {
        return tokenRepository
            .findFirstByTipoUtilizadorAndUtilizadorIdAndFinalidadeAndCodigoIsNotNullOrderByDataCriacaoDesc(
                tipo, utilizadorId, finalidade)
            .map(t -> t.getDataCriacao().plus(intervalo).isAfter(LocalDateTime.now()))
            .orElse(false);
    }

    /**
     * Confere o código escrito pelo utilizador contra o último código pendente.
     * Cada falha conta uma tentativa; ao chegar a {@link #MAX_TENTATIVAS} o código é anulado.
     * Chamar sempre dentro de uma transacção: o lock da linha tem de durar até ao fim da conferência.
     */
    @Transactional
    public ResultadoCodigo verificarCodigo(TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade, String codigoIntroduzido) {
        // "123 456" e "123456 " valem como 123456; o que não for 6 dígitos nem chega a gastar uma tentativa.
        String codigo = codigoIntroduzido == null ? "" : codigoIntroduzido.replaceAll("\\s", "");
        if (!SEIS_DIGITOS.matcher(codigo).matches()) {
            return ResultadoCodigo.de(EstadoCodigo.FORMATO_INVALIDO);
        }

        List<TokenVerificacao> pendentes = tokenRepository
            .findByTipoUtilizadorAndUtilizadorIdAndFinalidadeAndUsadoFalseAndCodigoIsNotNullOrderByDataCriacaoDesc(
                tipo, utilizadorId, finalidade);
        if (pendentes.isEmpty() || !pendentes.get(0).isValido()) {
            return ResultadoCodigo.de(EstadoCodigo.EXPIRADO);
        }
        TokenVerificacao t = pendentes.get(0);

        if (iguais(t.getCodigo(), codigo)) {
            marcarUsado(t);
            return ResultadoCodigo.de(EstadoCodigo.CONFIRMADO);
        }

        t.setTentativas(t.getTentativas() + 1);
        if (t.getTentativas() >= MAX_TENTATIVAS) {
            t.setUsado(true);
            tokenRepository.save(t);
            return ResultadoCodigo.de(EstadoCodigo.BLOQUEADO);
        }
        tokenRepository.save(t);
        return new ResultadoCodigo(EstadoCodigo.INCORRETO, MAX_TENTATIVAS - t.getTentativas());
    }

    // ─── Auxiliares ──────────────────────────────────────────────────────

    private TokenVerificacao novo(TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade, Duration validade) {
        TokenVerificacao t = new TokenVerificacao();
        t.setToken(UUID.randomUUID().toString().replace("-", ""));
        t.setTipoUtilizador(tipo);
        t.setUtilizadorId(utilizadorId);
        t.setFinalidade(finalidade);
        t.setDataCriacao(LocalDateTime.now());
        t.setDataExpiracao(LocalDateTime.now().plus(validade));
        t.setUsado(false);
        return t;
    }

    /** Comparação em tempo constante (não revela quantos dígitos estavam certos). */
    private static boolean iguais(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
