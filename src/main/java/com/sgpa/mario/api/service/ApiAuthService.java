package com.example.demo.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.api.config.JwtService;
import com.example.demo.api.config.UsuarioAtual;
import com.example.demo.api.dto.AuthDtos.AdminLoginRequest;
import com.example.demo.api.dto.AuthDtos.LoginRequest;
import com.example.demo.api.dto.AuthDtos.RedefinirSenhaRequest;
import com.example.demo.api.dto.AuthDtos.TokenResponse;
import com.example.demo.api.dto.UsuarioDtos.UsuarioResponse;
import com.example.demo.api.exception.ApiException;
import com.example.demo.web.domain.Admin;
import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Produtor;
import com.example.demo.api.repository.AdminRepository;
import com.example.demo.api.repository.CompradorRepository;
import com.example.demo.api.repository.ProdutorRepository;
import com.example.demo.api.service.ConfirmacaoEmailService;
import com.example.demo.api.service.LoginAttemptService;
import com.example.demo.api.service.PasswordEncoderService;
import com.example.demo.api.service.PasswordResetService;
import com.example.demo.api.service.exception.RecursoNaoEncontradoException;

/** Autenticação sem sessão: valida credenciais e emite JWT. Reutiliza BCrypt e o bloqueio por tentativas. */
@Service
public class ApiAuthService {

    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoderService passwords;
    private final LoginAttemptService tentativas;
    private final JwtService jwt;
    private final ApiUsuarioService usuarios;
    private final ConfirmacaoEmailService confirmacaoEmailService;
    private final PasswordResetService passwordResetService;
    private final String baseUrl;

    public ApiAuthService(ProdutorRepository produtorRepository, CompradorRepository compradorRepository,
                          AdminRepository adminRepository, PasswordEncoderService passwords,
                          LoginAttemptService tentativas, JwtService jwt, ApiUsuarioService usuarios,
                          ConfirmacaoEmailService confirmacaoEmailService,
                          PasswordResetService passwordResetService,
                          @Value("${app.base-url}") String baseUrl) {
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.adminRepository = adminRepository;
        this.passwords = passwords;
        this.tentativas = tentativas;
        this.jwt = jwt;
        this.usuarios = usuarios;
        this.confirmacaoEmailService = confirmacaoEmailService;
        this.passwordResetService = passwordResetService;
        this.baseUrl = baseUrl;
    }

    // ─── Login Produtor / Comprador ──────────────────────────────────────

    @Transactional
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim();
        String tipo = (req.tipo() == null || req.tipo().isBlank()) ? null : req.tipo().trim().toUpperCase(Locale.ROOT);
        if (tipo != null && !UsuarioAtual.PRODUTOR.equals(tipo) && !UsuarioAtual.COMPRADOR.equals(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TIPO_INVALIDO", "O tipo deve ser PRODUTOR ou COMPRADOR.");
        }
        verificarBloqueio(email);

        if (tipo == null || UsuarioAtual.PRODUTOR.equals(tipo)) {
            Optional<Produtor> op = produtorRepository.findByEmail(email);
            if (op.isPresent() && passwords.matches(req.senha(), op.get().getSenha())) {
                Produtor p = op.get();
                validarConta(p.getEmailVerificado(), p.isContaAtiva());
                tentativas.registarSucesso(email);
                if (!passwords.isHash(p.getSenha())) p.setSenha(passwords.encode(req.senha()));
                p.setUltimoAcesso(LocalDateTime.now());
                produtorRepository.save(p);
                return emitir(UsuarioAtual.PRODUTOR, p.getId(), p.getNomeCompleto(),
                    List.of("PRODUTOR"), usuarios.paraProdutor(p));
            }
        }
        if (tipo == null || UsuarioAtual.COMPRADOR.equals(tipo)) {
            Optional<Comprador> oc = compradorRepository.findByEmail(email);
            if (oc.isPresent() && passwords.matches(req.senha(), oc.get().getSenha())) {
                Comprador c = oc.get();
                validarConta(c.getEmailVerificado(), c.isContaAtiva());
                tentativas.registarSucesso(email);
                if (!passwords.isHash(c.getSenha())) c.setSenha(passwords.encode(req.senha()));
                c.setUltimoAcesso(LocalDateTime.now());
                compradorRepository.save(c);
                return emitir(UsuarioAtual.COMPRADOR, c.getId(), c.getNomeCompleto(),
                    List.of("COMPRADOR"), usuarios.paraComprador(c));
            }
        }
        tentativas.registarFalha(email);
        throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha incorrectos.");
    }

    // ─── Login Admin ─────────────────────────────────────────────────────

    @Transactional
    public TokenResponse loginAdmin(AdminLoginRequest req) {
        String email = req.email().trim();
        String chave = "admin:" + email;
        verificarBloqueio(chave);
        Optional<Admin> oa = adminRepository.findByEmail(email);
        if (oa.isPresent() && passwords.matches(req.senha(), oa.get().getSenha())) {
            Admin a = oa.get();
            tentativas.registarSucesso(chave);
            if (!passwords.isHash(a.getSenha())) a.setSenha(passwords.encode(req.senha()));
            a.setUltimoAcesso(LocalDateTime.now());
            adminRepository.save(a);
            List<String> roles = a.isSuperAdmin() ? List.of("ADMIN", "SUPER_ADMIN") : List.of("ADMIN");
            return emitir(UsuarioAtual.ADMIN, a.getId(), a.getNomeCompleto(), roles, ApiMapper.admin(a));
        }
        tentativas.registarFalha(chave);
        throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha incorrectos.");
    }

    // ─── Trocar entre Produtor e Comprador (mesmo e-mail) ────────────────

    @Transactional(readOnly = true)
    public TokenResponse trocarPerfil(UsuarioAtual.Dados atual, String destino) {
        String d = destino == null ? "" : destino.trim().toUpperCase(Locale.ROOT);
        boolean validoParaTroca = !UsuarioAtual.ADMIN.equals(atual.tipo())
            && (UsuarioAtual.PRODUTOR.equals(d) || UsuarioAtual.COMPRADOR.equals(d))
            && !d.equals(atual.tipo());
        if (!validoParaTroca) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TROCA_INVALIDA",
                "Só é possível trocar entre os perfis PRODUTOR e COMPRADOR (o perfil de destino tem de ser o outro).");
        }
        String email = usuarios.emailDe(atual);

        if (UsuarioAtual.PRODUTOR.equals(d)) {
            Produtor p = produtorRepository.findByEmail(email).orElseThrow(() -> new RecursoNaoEncontradoException(
                "Ainda não tem conta de Produtor. Registe-se em POST /api/v1/produtores."));
            validarConta(p.getEmailVerificado(), p.isContaAtiva());
            return emitir(UsuarioAtual.PRODUTOR, p.getId(), p.getNomeCompleto(),
                List.of("PRODUTOR"), usuarios.paraProdutor(p));
        }
        Comprador c = compradorRepository.findByEmail(email).orElseThrow(() -> new RecursoNaoEncontradoException(
            "Ainda não tem conta de Comprador. Registe-se em POST /api/v1/compradores."));
        validarConta(c.getEmailVerificado(), c.isContaAtiva());
        return emitir(UsuarioAtual.COMPRADOR, c.getId(), c.getNomeCompleto(),
            List.of("COMPRADOR"), usuarios.paraComprador(c));
    }

    // ─── Confirmação de e-mail e recuperação de senha ────────────────────

    public void confirmarEmail(String token) {
        if (!confirmacaoEmailService.confirmar(token)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO", "O link de confirmação é inválido ou expirou.");
        }
    }

    public com.example.demo.api.dto.AuthDtos.ConfirmacaoResponse consultarConfirmacao(String token) {
        return confirmacaoEmailService.consultar(token)
            .map(d -> new com.example.demo.api.dto.AuthDtos.ConfirmacaoResponse(d.tipo(), d.nome(), d.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO",
                "O link é inválido, já foi usado, a conta já foi confirmada, ou expirou."));
    }

    /** "Não fui eu": cancela o registo e apaga a conta ainda não confirmada. */
    public void rejeitarConta(String token) {
        if (!confirmacaoEmailService.rejeitar(token)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO",
                "O link é inválido, já foi usado, a conta já foi confirmada, ou expirou.");
        }
    }

    public void reenviarConfirmacao(String email) {
        confirmacaoEmailService.reenviarConfirmacao(email.trim(), baseUrl);
    }

    public void recuperarSenha(String email) {
        passwordResetService.solicitarRecuperacao(email.trim(), baseUrl);
    }

    public void redefinirSenha(RedefinirSenhaRequest r) {
        passwordResetService.redefinirSenha(r.token(), r.novaSenha(), r.confirmarSenha());
    }

    // ─── Auxiliares ──────────────────────────────────────────────────────

    private void verificarBloqueio(String chave) {
        if (tentativas.estaBloqueado(chave)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_BLOQUEADO",
                "Demasiadas tentativas falhadas. Tente novamente dentro de "
                + tentativas.minutosRestantes(chave) + " minuto(s).");
        }
    }

    private void validarConta(Boolean emailVerificado, boolean ativa) {
        if (Boolean.FALSE.equals(emailVerificado)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NAO_CONFIRMADO",
                "Confirme o seu e-mail antes de entrar (pode pedir novo link em /api/v1/auth/reenviar-confirmacao).");
        }
        if (!ativa) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CONTA_SUSPENSA",
                "A sua conta está suspensa. Contacte a administração.");
        }
    }

    private TokenResponse emitir(String tipo, Long id, String nome, List<String> roles, UsuarioResponse usuario) {
        JwtService.TokenEmitido t = jwt.emitir(tipo, id, nome, roles);
        return new TokenResponse(t.valor(), "Bearer", t.expiraEmSegundos(), usuario);
    }
}
