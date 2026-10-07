package com.example.demo.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") String email,
        @NotBlank(message = "Indique a senha.") String senha,
        @Schema(description = "PRODUTOR ou COMPRADOR. Opcional: por omissão tenta Produtor e depois Comprador.",
                example = "COMPRADOR") String tipo) {}

    public record AdminLoginRequest(
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") String email,
        @NotBlank(message = "Indique a senha.") String senha) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                                UsuarioDtos.UsuarioResponse usuario) {}

    public record TrocarPerfilRequest(
        @NotBlank(message = "Indique o perfil de destino.")
        @Schema(description = "PRODUTOR ou COMPRADOR", example = "PRODUTOR") String tipo) {}

    public record EmailRequest(
        @NotBlank(message = "Indique o e-mail.") @Email(message = "E-mail inválido.") String email) {}

    public record TokenRequest(@NotBlank(message = "Indique o token.") String token) {}

    public record RedefinirSenhaRequest(
        @NotBlank(message = "Indique o token.") String token,
        @NotBlank(message = "Indique a nova senha.")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.") String novaSenha,
        @NotBlank(message = "Confirme a nova senha.") String confirmarSenha) {}

    public record MensagemResponse(String mensagem) {}

    /** Quem vai ser confirmado/cancelado: mostrar ao utilizador antes de decidir. */
    public record ConfirmacaoResponse(String tipo, String nome, String email) {}
}
