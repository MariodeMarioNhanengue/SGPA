package com.example.demo.api.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.web.domain.Admin;
import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.domain.TokenVerificacao;
import com.example.demo.web.domain.TokenVerificacao.Finalidade;
import com.example.demo.web.domain.TokenVerificacao.TipoUtilizador;
import com.example.demo.api.repository.AdminRepository;
import com.example.demo.api.repository.CompradorRepository;
import com.example.demo.api.repository.ProdutorRepository;
import com.example.demo.api.service.exception.RegraNegocioException;

/**
 * Fluxo de "esqueci-me da senha": pedir a recuperação por email, e depois
 * definir uma nova senha a partir do link recebido.
 *
 * Por segurança, {@link #solicitarRecuperacao} nunca revela se um dado email
 * existe ou não no sistema — o comportamento observável é sempre o mesmo
 * ("se existir uma conta com este email, enviámos um link"), evitando que
 * alguém use este formulário para descobrir contas registadas.
 */
@Service
public class PasswordResetService {

    private static final Duration VALIDADE_TOKEN = Duration.ofMinutes(30);

    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final AdminRepository adminRepository;
    private final TokenVerificacaoService tokenVerificacaoService;
    private final PasswordEncoderService passwordEncoderService;
    private final EmailService emailService;

    public PasswordResetService(ProdutorRepository produtorRepository,
                                CompradorRepository compradorRepository,
                                AdminRepository adminRepository,
                                TokenVerificacaoService tokenVerificacaoService,
                                PasswordEncoderService passwordEncoderService,
                                EmailService emailService) {
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.adminRepository = adminRepository;
        this.tokenVerificacaoService = tokenVerificacaoService;
        this.passwordEncoderService = passwordEncoderService;
        this.emailService = emailService;
    }

    public void solicitarRecuperacao(String email, String baseUrl) {
        if (email == null || email.isBlank()) return;

        Optional<Produtor> produtor = produtorRepository.findByEmail(email);
        if (produtor.isPresent()) {
            enviarLink(TipoUtilizador.PRODUTOR, produtor.get().getId(), produtor.get().getNome(), email, baseUrl);
            return;
        }

        Optional<Comprador> comprador = compradorRepository.findByEmail(email);
        if (comprador.isPresent()) {
            enviarLink(TipoUtilizador.COMPRADOR, comprador.get().getId(), comprador.get().getNome(), email, baseUrl);
            return;
        }

        Optional<Admin> admin = adminRepository.findByEmail(email);
        admin.ifPresent(a -> enviarLink(TipoUtilizador.ADMIN, a.getId(), a.getNome(), email, baseUrl));

        // Se nenhum existir, não fazemos nada — nem enviamos email, nem avisamos o chamador.
    }

    private void enviarLink(TipoUtilizador tipo, Long id, String nome, String email, String baseUrl) {
        String token = tokenVerificacaoService.gerarToken(tipo, id, Finalidade.RESET_SENHA, VALIDADE_TOKEN);
        String link = baseUrl + "/redefinir-senha?token=" + token;
        String assunto = "Recuperação de senha — SGPA";
        String corpo = "Olá " + nome + ",\n\n"
            + "Recebemos um pedido para redefinir a sua senha no SGPA.\n\n"
            + "Para escolher uma nova senha, aceda a este link (válido por 30 minutos):\n"
            + link + "\n\n"
            + "Se não foi você que pediu isto, ignore este email — a sua senha actual continua válida.";
        emailService.enviar(email, assunto, corpo);
    }

    /** Valida o token e devolve o tipo/id do utilizador a quem pertence, sem ainda alterar nada. */
    public Optional<TokenVerificacao> validarToken(String token) {
        return tokenVerificacaoService.validar(token, Finalidade.RESET_SENHA);
    }

    public void redefinirSenha(String token, String novaSenha, String confirmarSenha) {
        if (novaSenha == null || novaSenha.isBlank() || novaSenha.length() < 6) {
            throw new RegraNegocioException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (!novaSenha.equals(confirmarSenha)) {
            throw new RegraNegocioException("A nova senha e a confirmação não coincidem.");
        }

        TokenVerificacao t = validarToken(token)
            .orElseThrow(() -> new RegraNegocioException("Este link de recuperação é inválido ou já expirou. Peça um novo."));

        String senhaHash = passwordEncoderService.encode(novaSenha);

        switch (t.getTipoUtilizador()) {
            case PRODUTOR -> produtorRepository.findById(t.getUtilizadorId()).ifPresent(p -> {
                p.setSenha(senhaHash);
                produtorRepository.save(p);
            });
            case COMPRADOR -> compradorRepository.findById(t.getUtilizadorId()).ifPresent(c -> {
                c.setSenha(senhaHash);
                compradorRepository.save(c);
            });
            case ADMIN -> adminRepository.findById(t.getUtilizadorId()).ifPresent(a -> {
                a.setSenha(senhaHash);
                adminRepository.save(a);
            });
        }

        tokenVerificacaoService.marcarUsado(t);
    }
}
