package com.example.demo.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.api.config.UsuarioAtual;
import com.example.demo.api.dto.AuthDtos.AdminLoginRequest;
import com.example.demo.api.dto.AuthDtos.ConfirmacaoResponse;
import com.example.demo.api.dto.AuthDtos.EmailRequest;
import com.example.demo.api.dto.AuthDtos.LoginRequest;
import com.example.demo.api.dto.AuthDtos.MensagemResponse;
import com.example.demo.api.dto.AuthDtos.RedefinirSenhaRequest;
import com.example.demo.api.dto.AuthDtos.TokenRequest;
import com.example.demo.api.dto.AuthDtos.TokenResponse;
import com.example.demo.api.dto.AuthDtos.TrocarPerfilRequest;
import com.example.demo.api.service.ApiAuthService;

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

    @GetMapping("/confirmacao")
    @Operation(summary = "Consulta o token de confirmação (não o consome) para mostrar quem vai ser confirmado")
    public ConfirmacaoResponse consultarConfirmacao(@RequestParam String token) {
        return auth.consultarConfirmacao(token);
    }

    @PostMapping("/confirmar-email")
    @Operation(summary = "Confirma o e-mail da conta")
    public MensagemResponse confirmarEmail(@Valid @RequestBody TokenRequest req) {
        auth.confirmarEmail(req.token());
        return new MensagemResponse("E-mail confirmado. Já pode entrar.");
    }

    @PostMapping("/rejeitar-conta")
    @Operation(summary = "\"Não fui eu\": cancela o registo de uma conta ainda não confirmada")
    public MensagemResponse rejeitarConta(@Valid @RequestBody TokenRequest req) {
        auth.rejeitarConta(req.token());
        return new MensagemResponse("Registo cancelado.");
    }

    @PostMapping("/reenviar-confirmacao")
    @Operation(summary = "Reenvia o e-mail de confirmação")
    public MensagemResponse reenviarConfirmacao(@Valid @RequestBody EmailRequest req) {
        auth.reenviarConfirmacao(req.email());
        // Resposta neutra: não revela se o e-mail existe.
        return new MensagemResponse("Se existir uma conta por confirmar com este e-mail, enviámos um novo link.");
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
