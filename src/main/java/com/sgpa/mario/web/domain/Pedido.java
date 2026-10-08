package com.example.demo.web.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "pedido")
public class Pedido extends AbstractEntity<Long> {

    @Column(name = "data_pedido")
    private LocalDateTime dataPedido;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @Column(name = "quantidade", precision = 10, scale = 2)
    private java.math.BigDecimal quantidade;

    // ======================
    // RELACIONAMENTOS
    // ======================

    @ManyToOne
    @JoinColumn(name = "Comprador_idComprador")
    private Comprador comprador;

    // ✅ ADICIONADO (ESSENCIAL)
    @ManyToOne
    @JoinColumn(name = "oferta_id")
    private Oferta oferta;

    // ======================
    // ESTADO
    // ======================
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    // ======================
    // JUSTIFICAÇÃO DE REJEIÇÃO
    // ======================
    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    // ======================
    // JUSTIFICAÇÃO DE CANCELAMENTO (pelo comprador)
    // ======================
    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    // ======================
    // NÚMERO DE PAGAMENTO DO PRODUTOR
    // (número que o produtor fornece ao comprador para efectuar a transação)
    // ======================
    @Column(name = "numero_pagamento_produtor", length = 50)
    private String numeroPagamentoProdutor;
public enum EstadoPedido {
    PENDENTE,
    AGUARDANDO_PAGAMENTO,
    AGUARDANDO_APROVACAO,
    ACEITE,
    REJEITADO,
    CANCELADO
}
    // ======================
    // GETTERS E SETTERS
    // ======================

    public java.math.BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(java.math.BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDateTime dataPedido) {
        this.dataPedido = dataPedido;
    }

    public LocalDateTime getDataResposta() {
        return dataResposta;
    }

    public void setDataResposta(LocalDateTime dataResposta) {
        this.dataResposta = dataResposta;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public Comprador getComprador() {
        return comprador;
    }

    public void setComprador(Comprador comprador) {
        this.comprador = comprador;
    }

    public Oferta getOferta() {
        return oferta;
    }

    public void setOferta(Oferta oferta) {
        this.oferta = oferta;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public String getNumeroPagamentoProdutor() {
        return numeroPagamentoProdutor;
    }

    public void setNumeroPagamentoProdutor(String numeroPagamentoProdutor) {
        this.numeroPagamentoProdutor = numeroPagamentoProdutor;
    }
}