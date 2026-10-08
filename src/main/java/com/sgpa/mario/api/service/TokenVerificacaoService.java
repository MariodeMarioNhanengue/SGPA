package com.sgpa.mario.api.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import com.sgpa.mario.web.domain.TokenVerificacao;
import com.sgpa.mario.web.domain.TokenVerificacao.*;
import com.sgpa.mario.api.repository.TokenVerificacaoRepository;

/**
 * Geração e validação de tokens de uso único (recuperação de senha,
 * confirmação de email). Não sabe nada sobre email ou sobre as entidades
 * Produtor/Comprador/Admin em si — apenas gere o ciclo de vida do token.
 */
@Service
public class TokenVerificacaoService {

    private final TokenVerificacaoRepository tokenRepository;

    public TokenVerificacaoService(TokenVerificacaoRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String gerarToken(TipoUtilizador tipo, Long utilizadorId, Finalidade finalidade, Duration validade) {
        TokenVerificacao t = new TokenVerificacao();
        t.setToken(UUID.randomUUID().toString().replace("-", ""));
        t.setTipoUtilizador(tipo);
        t.setUtilizadorId(utilizadorId);
        t.setFinalidade(finalidade);
        t.setDataCriacao(LocalDateTime.now());
        t.setDataExpiracao(LocalDateTime.now().plus(validade));
        t.setUsado(false);
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
}
