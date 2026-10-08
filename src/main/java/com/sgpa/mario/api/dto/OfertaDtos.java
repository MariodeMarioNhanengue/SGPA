package com.sgpa.mario.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sgpa.mario.web.domain.Oferta.EstadoOferta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OfertaDtos {

    private OfertaDtos() {}

    public record OfertaRequest(
        @NotBlank(message = "Indique o nome do produto.") @Size(max = 255) String nomeProduto,
        @NotBlank(message = "Indique a categoria.") @Size(max = 255) String categoria,
        @NotBlank(message = "Indique a unidade (ex.: kg, saco, caixa).") @Size(max = 50) String unidade,
        @NotNull(message = "Indique a quantidade total.")
        @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Quantidade inválida.") BigDecimal quantidadeTotal,
        @DecimalMin(value = "0.00", message = "A quantidade restante não pode ser negativa.")
        @Digits(integer = 8, fraction = 2, message = "Quantidade inválida.") BigDecimal quantidadeRestante,
        @NotNull(message = "Indique o preço por unidade.")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Preço inválido.") BigDecimal preco,
        @Size(max = 5000) String descricao,
        LocalDateTime dataValidade,
        EstadoOferta estado,
        String provincia, String distrito, String bairro) {}

    public record EstadoOfertaRequest(@NotNull(message = "Indique o estado.") EstadoOferta estado) {}

    public record ProdutorResumo(Long id, String nome) {}

    public record LocalizacaoResumo(Long provinciaId, String provincia, Long distritoId, String distrito,
                                    Long bairroId, String bairro) {}

    public record OfertaResponse(
        Long id, String nomeProduto, String categoria, String unidade,
        BigDecimal quantidadeTotal, BigDecimal quantidadeRestante, BigDecimal preco,
        String descricao, String imagemUrl, EstadoOferta estado,
        LocalDateTime dataPublicacao, LocalDateTime dataValidade,
        ProdutorResumo produtor, LocalizacaoResumo localizacao) {}
}
