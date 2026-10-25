package com.sgpa.mario.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgpa.mario.api.config.UsuarioAtual;
import com.sgpa.mario.api.dto.AuthDtos.AdminLoginRequest;
import com.sgpa.mario.api.dto.AuthDtos.ConfirmarEmailRequest;
import com.sgpa.mario.api.dto.AuthDtos.EmailRequest;
import com.sgpa.mario.api.dto.AuthDtos.LoginRequest;
import com.sgpa.mario.api.dto.AuthDtos.MensagemResponse;
import com.sgpa.mario.api.dto.AuthDtos.RedefinirSenhaRequest;
import com.sgpa.mario.api.dto.AuthDtos.TokenResponse;
import com.sgpa.mario.api.dto.AuthDtos.TrocarPerfilRequest;
import com.sgpa.mario.api.service.ApiAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Autenticação da API REST: emissão de JWT, confirmação de e-mail e recuperação de senha.
 * As rotas públicas estão declaradas em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthApiController {

    private final ApiAuthService auth;

    public AuthApiController(ApiAuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    @Operation(summary = "Login de Produtor/Comprador — devolve o JWT")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Login de Administrador — devolve o JWT")
    public TokenResponse loginAdmin(@Valid @RequestBody AdminLoginRequest req) {
        return auth.loginAdmin(req);
    }

    @PostMapping("/trocar-perfil")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Troca entre os perfis Produtor e Comprador (mesmo e-mail) e devolve novo JWT")
    public TokenResponse trocarPerfil(@Valid @RequestBody TrocarPerfilRequest req) {
        return auth.trocarPerfil(UsuarioAtual.obter(), req.tipo());
    }

    @PostMapping("/confirmar-email")
    @Operation(summary = "Confirma o e-mail da conta com o código de 6 dígitos recebido por e-mail",
        description = "Máximo de 5 tentativas por código; depois é preciso pedir um novo em /auth/reenviar-confirmacao.")
    public MensagemResponse confirmarEmail(@Valid @RequestBody ConfirmarEmailRequest req) {
        auth.confirmarEmail(req.email(), req.codigo());
        return new MensagemResponse("E-mail confirmado. Já pode entrar.");
    }

    @PostMapping("/reenviar-confirmacao")
    @Operation(summary = "Envia um novo código de confirmação por e-mail (o anterior deixa de valer)",
        description = "No máximo um por minuto por conta.")
    public MensagemResponse reenviarConfirmacao(@Valid @RequestBody EmailRequest req) {
        auth.reenviarConfirmacao(req.email());
        // Resposta neutra: não revela se o e-mail existe.
        return new MensagemResponse("Se existir uma conta por confirmar com este e-mail, enviámos um novo código.");
    }

    @PostMapping("/recuperar-senha")
    @Operation(summary = "Pede o e-mail de recuperação de senha")
    public MensagemResponse recuperarSenha(@Valid @RequestBody EmailRequest req) {
        auth.recuperarSenha(req.email());
        return new MensagemResponse("Se o e-mail estiver registado, enviámos as instruções de recuperação.");
    }

    @PostMapping("/redefinir-senha")
    @Operation(summary = "Define a nova senha com o token recebido por e-mail")
    public MensagemResponse redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest req) {
        auth.redefinirSenha(req);
        return new MensagemResponse("Senha redefinida com sucesso.");
    }
}
