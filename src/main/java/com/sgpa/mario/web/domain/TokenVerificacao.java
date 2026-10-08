package com.example.demo.web.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Token de uso único, com expiração, para fluxos de "prova de posse do email":
 * recuperação de senha e confirmação de conta no registo.
 *
 * Não guarda uma FK directa para Produtor/Comprador/Admin (que são tabelas
 * distintas) — em vez disso guarda o tipo de utilizador e o respectivo id,
 * resolvidos no serviço que consome o token.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "token_verificacao")
public class TokenVerificacao extends AbstractEntity<Long> {

    public enum TipoUtilizador { PRODUTOR, COMPRADOR, ADMIN }

    public enum Finalidade { RESET_SENHA, CONFIRMACAO_EMAIL }

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUtilizador tipoUtilizador;

    @Column(nullable = false)
    private Long utilizadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Finalidade finalidade;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(nullable = false)
    private boolean usado = false;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public TipoUtilizador getTipoUtilizador() { return tipoUtilizador; }
    public void setTipoUtilizador(TipoUtilizador tipoUtilizador) { this.tipoUtilizador = tipoUtilizador; }

    public Long getUtilizadorId() { return utilizadorId; }
    public void setUtilizadorId(Long utilizadorId) { this.utilizadorId = utilizadorId; }

    public Finalidade getFinalidade() { return finalidade; }
    public void setFinalidade(Finalidade finalidade) { this.finalidade = finalidade; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDateTime dataExpiracao) { this.dataExpiracao = dataExpiracao; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }

    public boolean isValido() {
        return !usado && dataExpiracao != null && LocalDateTime.now().isBefore(dataExpiracao);
    }
}
