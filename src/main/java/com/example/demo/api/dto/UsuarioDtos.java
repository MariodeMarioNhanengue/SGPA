package com.example.demo.api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UsuarioDtos {

    private UsuarioDtos() {}

    /** Vista de um utilizador (nunca inclui a senha). Campos que não se aplicam ao tipo vêm a null. */
    public record UsuarioResponse(
        String tipo, Long id, String nome, String apelido, String email, String telefone, String nuit,
        String fotoPerfil, Boolean emailVerificado, Boolean ativo, Boolean superAdmin,
        String provincia, String distrito, String bairro,
        String outroPerfilDisponivel, LocalDateTime ultimoAcesso) {}

    public record RegistoCompradorRequest(
        @NotBlank(message = "Indique o nome.") @Size(max = 150) String nome,
        @NotBlank(message = "Indique o apelido.") @Size(max = 150) String apelido,
        @NotBlank(message = "Indique o telefone.") @Size(max = 20) String telefone,
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 100) String email,
        @NotBlank(message = "Indique a senha.")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.") String senha,
        @Size(max = 20) String nuit,
        String provincia, String distrito, String bairro) {}

    public record RegistoProdutorRequest(
        @NotBlank(message = "Indique o nome.") @Size(max = 150) String nome,
        @NotBlank(message = "Indique o apelido.") @Size(max = 150) String apelido,
        @NotBlank(message = "Indique o telefone.") @Size(max = 20) String telefone,
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 255) String email,
        @NotBlank(message = "Indique a senha.")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.") String senha,
        @Size(max = 20) String nuit) {}

    public record AtualizarPerfilRequest(
        @NotBlank(message = "Indique o nome.") @Size(max = 150) String nome,
        @NotBlank(message = "Indique o apelido.") @Size(max = 150) String apelido,
        @Size(max = 20) String telefone,
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 255) String email,
        @Size(max = 20) String nuit,
        String provincia, String distrito, String bairro) {}

    public record AlterarSenhaRequest(
        @NotBlank(message = "Indique a senha actual.") String senhaAtual,
        @NotBlank(message = "Indique a nova senha.")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.") String novaSenha) {}

    public record ResumoProdutorResponse(long totalOfertas, long totalPedidos,
                                         long pedidosPendentes, long pedidosAceites) {}
}
