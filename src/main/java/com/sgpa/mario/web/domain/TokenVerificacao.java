package com.sgpa.mario.web.domain;

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
 *
 * Na confirmação de conta (Finalidade.CONFIRMACAO_EMAIL) o utilizador não recebe o
 * {@code token} mas sim um {@code codigo} curto de 6 dígitos, que escreve no sistema.
 * Como 6 dígitos não são únicos, o código só é procurado em conjunto com o tipo e o id
 * do utilizador, e {@code tentativas} limita quantas vezes se pode tentar adivinhá-lo.
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

    /** Código de 6 dígitos enviado por e-mail (só na confirmação de conta; null nos restantes tokens). */
    @Column(length = 6)
    private String codigo;

    /** Tentativas falhadas de introduzir {@link #codigo}. */
    @Column(nullable = false, columnDefinition = "int not null default 0")
    private int tentativas = 0;

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

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public int getTentativas() { return tentativas; }
    public void setTentativas(int tentativas) { this.tentativas = tentativas; }

    public boolean isValido() {
        return !usado && dataExpiracao != null && LocalDateTime.now().isBefore(dataExpiracao);
    }
}
