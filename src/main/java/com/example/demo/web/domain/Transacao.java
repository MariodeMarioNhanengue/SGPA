package com.example.demo.web.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "transacao")
public class Transacao extends AbstractEntity<Long> {

    // ── PEDIDO ASSOCIADO ─────────────────────────────────────
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // ── MÉTODO DE PAGAMENTO ──────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "operadora", nullable = false)
    private Operadora operadora;

    // ── REFERÊNCIA (número de telemóvel ou referência bancária) ──
    @Column(name = "referencia", length = 100)
    private String referencia;

    // ── VALOR TOTAL ──────────────────────────────────────────
    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    // ── ESTADO ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTransacao estado;

    // ── DATAS ────────────────────────────────────────────────
    @Column(name = "data_transacao")
    private LocalDateTime dataTransacao;

    @Column(name = "data_confirmacao")
    private LocalDateTime dataConfirmacao;

    // ── ENUMS ────────────────────────────────────────────────
    public enum MetodoPagamento {
        CONTA_MOVEL,
        BANCO
    }

    public enum Operadora {
        MPESA,
        EMOLA,
        MKESH,
        BCI,
        BIM,
        STANDARD_BANK,
        VISA,
        OUTROS
    }

    public enum EstadoTransacao {
        PENDENTE,       // aguarda confirmação do produtor
        CONFIRMADA,     // produtor aceitou
        CANCELADA       // produtor rejeitou
    }

    // ── LIFECYCLE ────────────────────────────────────────────
    @PrePersist
    public void prePersist() {
        if (dataTransacao == null) {
            dataTransacao = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoTransacao.PENDENTE;
        }
    }

    // ── GETTERS E SETTERS ────────────────────────────────────
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public MetodoPagamento getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }

    public Operadora getOperadora() { return operadora; }
    public void setOperadora(Operadora operadora) { this.operadora = operadora; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public EstadoTransacao getEstado() { return estado; }
    public void setEstado(EstadoTransacao estado) { this.estado = estado; }

    public LocalDateTime getDataTransacao() { return dataTransacao; }
    public void setDataTransacao(LocalDateTime dataTransacao) { this.dataTransacao = dataTransacao; }

    public LocalDateTime getDataConfirmacao() { return dataConfirmacao; }
    public void setDataConfirmacao(LocalDateTime dataConfirmacao) { this.dataConfirmacao = dataConfirmacao; }
}
