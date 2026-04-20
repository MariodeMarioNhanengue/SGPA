package com.example.demo.web.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "pedido")
public class Pedido extends AbstractEntity<Long> {

    @Column(name = "data_pedido")
    private LocalDateTime dataPedido;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @ManyToOne
    @JoinColumn(name = "Comprador_idComprador")
    private Comprador comprador;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    public enum EstadoPedido {
        PENDENTE,
        ACEITE,
        REJEITADO
    }

    // getters/setters
    public LocalDateTime getDataPedido() { return dataPedido; }
    public void setDataPedido(LocalDateTime dataPedido) { this.dataPedido = dataPedido; }

    public LocalDateTime getDataResposta() { return dataResposta; }
    public void setDataResposta(LocalDateTime dataResposta) { this.dataResposta = dataResposta; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public Comprador getComprador() { return comprador; }
    public void setComprador(Comprador comprador) { this.comprador = comprador; }
}