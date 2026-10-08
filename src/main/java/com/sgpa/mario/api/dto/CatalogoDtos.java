package com.sgpa.mario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CatalogoDtos {

    private CatalogoDtos() {}

    public record ProvinciaResponse(Long id, String nome) {}
    public record DistritoResponse(Long id, String nome, Long provinciaId) {}
    public record BairroResponse(Long id, String nome, Long distritoId) {}

    public record ProvinciaRequest(@NotBlank(message = "Indique o nome.") @Size(max = 100) String nome) {}

    public record DistritoRequest(
        @NotBlank(message = "Indique o nome.") @Size(max = 100) String nome,
        @NotNull(message = "Indique a província.") Long provinciaId) {}

    public record BairroRequest(
        @NotBlank(message = "Indique o nome.") @Size(max = 100) String nome,
        @NotNull(message = "Indique o distrito.") Long distritoId) {}

    public record CategoriaResponse(Long id, String nome, String descricao) {}

    public record ProdutoResponse(Long id, String nome, String descricao, String unidade, String categoria) {}
}
