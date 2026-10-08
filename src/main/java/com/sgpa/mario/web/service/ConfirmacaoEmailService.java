package com.example.demo.web.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.domain.TokenVerificacao;
import com.example.demo.web.domain.TokenVerificacao.Finalidade;
import com.example.demo.web.domain.TokenVerificacao.TipoUtilizador;
import com.example.demo.web.repository.CompradorRepository;
import com.example.demo.web.repository.ProdutorRepository;

/**
 * Envio e validação do link de confirmação de email enviado no registo de
 * uma nova conta (Produtor ou Comprador). Contas de Admin não passam por
 * este fluxo — são sempre criadas por outro administrador.
 */
@Service
public class ConfirmacaoEmailService {

    private static final Duration VALIDADE_TOKEN = Duration.ofHours(24);

    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final TokenVerificacaoService tokenVerificacaoService;
    private final EmailService emailService;

    public ConfirmacaoEmailService(ProdutorRepository produtorRepository,
                                   CompradorRepository compradorRepository,
                                   TokenVerificacaoService tokenVerificacaoService,
                                   EmailService emailService) {
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.tokenVerificacaoService = tokenVerificacaoService;
        this.emailService = emailService;
    }

    public void enviarConfirmacaoProdutor(Produtor produtor, String baseUrl) {
        enviar(TipoUtilizador.PRODUTOR, produtor.getId(), produtor.getNome(), produtor.getEmail(), baseUrl);
    }

    public void enviarConfirmacaoComprador(Comprador comprador, String baseUrl) {
        enviar(TipoUtilizador.COMPRADOR, comprador.getId(), comprador.getNome(), comprador.getEmail(), baseUrl);
    }

    private void enviar(TipoUtilizador tipo, Long id, String nome, String email, String baseUrl) {
        String token = tokenVerificacaoService.gerarToken(tipo, id, Finalidade.CONFIRMACAO_EMAIL, VALIDADE_TOKEN);
        String link = baseUrl + "/confirmar-email?token=" + token;
        String assunto = "Confirme o seu email — SGPA";
        String corpo = "Olá " + nome + ",\n\n"
            + "Obrigado por se registar no SGPA! Para activar a sua conta, confirme o seu email através deste link "
            + "(válido por 24 horas):\n\n"
            + link + "\n\n"
            + "Se não foi você que criou esta conta, pode ignorar este email.";
        emailService.enviar(email, assunto, corpo);
    }

    /** Reenvia o email de confirmação a uma conta ainda não confirmada, identificada pelo email. */
    public void reenviarConfirmacao(String email, String baseUrl) {
        produtorRepository.findByEmail(email)
            .filter(p -> Boolean.FALSE.equals(p.getEmailVerificado()))
            .ifPresent(p -> enviarConfirmacaoProdutor(p, baseUrl));

        compradorRepository.findByEmail(email)
            .filter(c -> Boolean.FALSE.equals(c.getEmailVerificado()))
            .ifPresent(c -> enviarConfirmacaoComprador(c, baseUrl));
    }

    /** Confirma a conta associada ao token. Devolve true se a confirmação foi feita com sucesso. */
    public boolean confirmar(String token) {
        return tokenVerificacaoService.validar(token, Finalidade.CONFIRMACAO_EMAIL)
            .map(this::aplicarConfirmacao)
            .orElse(false);
    }

    private boolean aplicarConfirmacao(TokenVerificacao t) {
        boolean aplicado = switch (t.getTipoUtilizador()) {
            case PRODUTOR -> produtorRepository.findById(t.getUtilizadorId()).map(p -> {
                p.setEmailVerificado(true);
                produtorRepository.save(p);
                return true;
            }).orElse(false);
            case COMPRADOR -> compradorRepository.findById(t.getUtilizadorId()).map(c -> {
                c.setEmailVerificado(true);
                compradorRepository.save(c);
                return true;
            }).orElse(false);
            case ADMIN -> false;
        };
        if (aplicado) {
            tokenVerificacaoService.marcarUsado(t);
        }
        return aplicado;
    }
}
