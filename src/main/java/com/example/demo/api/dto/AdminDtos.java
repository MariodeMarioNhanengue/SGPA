package com.example.demo.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminDtos {

    private AdminDtos() {}

    public record ResumoAdminResponse(long totalProdutores, long totalCompradores, long totalPedidos,
                                      long totalOfertas, long pedidosPendentes, long pedidosAceites,
                                      List<PedidoDtos.PedidoResponse> pedidosRecentes) {}

    public record CriarUtilizadorRequest(
        @NotBlank(message = "Indique o nome.") @Size(max = 150) String nome,
        @NotBlank(message = "Indique o apelido.") @Size(max = 150) String apelido,
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 100) String email,
        @Size(max = 20) String telefone,
        @Size(max = 20) String nuit,
        @NotBlank(message = "Indique a senha.")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.") String senha) {}

    public record CriarAdminRequest(
        @NotBlank(message = "Indique o nome.") @Size(max = 150) String nome,
        @NotBlank(message = "Indique o apelido.") @Size(max = 150) String apelido,
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") String email,
        @NotBlank(message = "Indique a senha.")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.") String senha) {}

    public record DetalheProdutorResponse(UsuarioDtos.UsuarioResponse produtor,
                                          long totalOfertas, long ofertasAtivas, long ofertasExpiradas,
                                          long ofertasInativas, long totalPedidos, long pedidosAceites,
                                          long pedidosPendentes, long pedidosRejeitados) {}

    public record DetalheCompradorResponse(UsuarioDtos.UsuarioResponse comprador,
                                           long totalPedidos, long pedidosAceites,
                                           long pedidosPendentes, long pedidosRejeitados) {}

    public record AdminLogResponse(Long id, String admin, String tipoAcao, String descricao,
                                   String nomeAfectado, String tipoAfectado, LocalDateTime dataAcao) {}
}
