package com.sgpa.mario.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sgpa.mario.web.domain.Pedido.EstadoPedido;
import com.sgpa.mario.web.domain.Transacao.EstadoTransacao;
import com.sgpa.mario.web.domain.Transacao.MetodoPagamento;
import com.sgpa.mario.web.domain.Transacao.Operadora;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PedidoDtos {

    private PedidoDtos() {}

    public record PedidoCreateRequest(
        @NotNull(message = "Indique a oferta.") Long ofertaId,
        @NotNull(message = "Indique a quantidade.")
        @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Quantidade inválida.") BigDecimal quantidade) {}

    public record AceitarPedidoRequest(
        @NotBlank(message = "Indique o número para receber o pagamento.") @Size(max = 50) String numeroPagamento) {}

    public record RejeitarPedidoRequest(
        @NotBlank(message = "Indique o motivo da rejeição.") @Size(max = 1000) String motivo) {}

    public record PagamentoRequest(
        @NotNull(message = "Indique o método de pagamento (CONTA_MOVEL ou BANCO).") MetodoPagamento metodoPagamento,
        @NotNull(message = "Indique a operadora.") Operadora operadora,
        @Size(max = 100) String referencia) {}

    public record TransacaoResponse(Long id, MetodoPagamento metodoPagamento, Operadora operadora,
                                    String referencia, BigDecimal valorTotal, EstadoTransacao estado,
                                    LocalDateTime dataTransacao, LocalDateTime dataConfirmacao) {}

    public record OfertaResumo(Long id, String nomeProduto, String categoria, String unidade,
                               BigDecimal preco, String imagemUrl, Long produtorId, String produtorNome) {}

    public record CompradorResumo(Long id, String nome, String telefone) {}

    public record PedidoResponse(
        Long id, EstadoPedido estado, BigDecimal quantidade, BigDecimal total,
        LocalDateTime dataPedido, LocalDateTime dataResposta,
        String motivoRejeicao, String motivoCancelamento, String numeroPagamentoProdutor,
        OfertaResumo oferta, CompradorResumo comprador, TransacaoResponse transacao) {}
}
