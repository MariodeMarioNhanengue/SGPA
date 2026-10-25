package com.sgpa.mario.api.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgpa.mario.web.domain.*;
import com.sgpa.mario.web.domain.TokenVerificacao.*;
import com.sgpa.mario.api.repository.*;
import com.sgpa.mario.api.service.TokenVerificacaoService.EstadoCodigo;
import com.sgpa.mario.api.service.TokenVerificacaoService.ResultadoCodigo;

/**
 * Confirmação do email no registo de uma nova conta (Produtor ou Comprador).
 *
 * O utilizador recebe por email um código de 6 dígitos e escreve-o no sistema; se estiver certo,
 * a conta fica activa de imediato. Contas de Admin não passam por este fluxo — são sempre
 * criadas por outro administrador.
 */
@Service
public class ConfirmacaoEmailService {

    public static final Duration VALIDADE_CODIGO = Duration.ofMinutes(15);

    /** Intervalo mínimo entre dois códigos para a mesma conta (evita spam de emails e de tentativas). */
    static final Duration INTERVALO_REENVIO = Duration.ofSeconds(60);

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

    // ─── Envio do código ─────────────────────────────────────────────────

    public void enviarConfirmacaoProdutor(Produtor produtor) {
        enviar(TipoUtilizador.PRODUTOR, produtor.getId(), produtor.getNome(), produtor.getEmail());
    }

    public void enviarConfirmacaoComprador(Comprador comprador) {
        enviar(TipoUtilizador.COMPRADOR, comprador.getId(), comprador.getNome(), comprador.getEmail());
    }

    private void enviar(TipoUtilizador tipo, Long id, String nome, String email) {
        String codigo = tokenVerificacaoService.gerarCodigo(tipo, id, Finalidade.CONFIRMACAO_EMAIL, VALIDADE_CODIGO);
        String assunto = "O seu código de confirmação — SGPA";
        String corpo = "Olá " + nome + ",\n\n"
            + "Obrigado por se registar no SGPA! O seu código de confirmação é:\n\n"
            + "        " + codigo + "\n\n"
            + "Introduza-o no SGPA para activar a sua conta. O código é válido por "
            + VALIDADE_CODIGO.toMinutes() + " minutos.\n\n"
            + "Se não foi você que criou esta conta, ignore este email: nada é activado sem este código.";
        emailService.enviar(email, assunto, corpo);
    }

    /**
     * Envia um código novo a uma conta ainda não confirmada, identificada pelo email (o anterior deixa de valer).
     * Se o último código foi enviado há menos de 1 minuto, não envia outro.
     */
    public void reenviarConfirmacao(String email) {
        produtorRepository.findByEmail(email)
            .filter(this::porConfirmar)
            .filter(p -> !enviadoRecentemente(TipoUtilizador.PRODUTOR, p.getId()))
            .ifPresent(this::enviarConfirmacaoProdutor);

        compradorRepository.findByEmail(email)
            .filter(this::porConfirmar)
            .filter(c -> !enviadoRecentemente(TipoUtilizador.COMPRADOR, c.getId()))
            .ifPresent(this::enviarConfirmacaoComprador);
    }

    private boolean enviadoRecentemente(TipoUtilizador tipo, Long id) {
        return tokenVerificacaoService.codigoEmitidoHaMenosDe(tipo, id, Finalidade.CONFIRMACAO_EMAIL, INTERVALO_REENVIO);
    }

    private boolean porConfirmar(Produtor p) { return Boolean.FALSE.equals(p.getEmailVerificado()); }

    private boolean porConfirmar(Comprador c) { return Boolean.FALSE.equals(c.getEmailVerificado()); }

    // ─── Confirmação com o código ────────────────────────────────────────

    /**
     * Confirma a conta indicada se o código estiver certo. É esta a via do site (a conta vem da sessão):
     * identifica a conta sem ambiguidade, mesmo que exista um Produtor e um Comprador com o mesmo email.
     */
    @Transactional
    public ResultadoCodigo confirmarCodigo(TipoUtilizador tipo, Long id, String codigo) {
        ResultadoCodigo r = tokenVerificacaoService.verificarCodigo(tipo, id, Finalidade.CONFIRMACAO_EMAIL, codigo);
        if (r.estado() != EstadoCodigo.CONFIRMADO) {
            return r;
        }
        // Código certo: activa a conta. Se entretanto ela desapareceu, não há nada a confirmar.
        return activar(tipo, id) ? r : ResultadoCodigo.de(EstadoCodigo.EXPIRADO);
    }

    /**
     * Confirma por email + código (via da API REST, que não tem sessão). Se o email tiver uma conta
     * de Produtor e outra de Comprador por confirmar, activa aquela cujo código bater certo.
     */
    @Transactional
    public ResultadoCodigo confirmarCodigoPorEmail(String email, String codigo) {
        String alvo = email == null ? "" : email.trim();
        ResultadoCodigo resultado = ResultadoCodigo.de(EstadoCodigo.EXPIRADO);

        Optional<Produtor> produtor = produtorRepository.findByEmail(alvo).filter(this::porConfirmar);
        if (produtor.isPresent()) {
            ResultadoCodigo r = confirmarCodigo(TipoUtilizador.PRODUTOR, produtor.get().getId(), codigo);
            if (r.estado() == EstadoCodigo.CONFIRMADO) return r;
            resultado = maisInformativo(resultado, r);
        }

        Optional<Comprador> comprador = compradorRepository.findByEmail(alvo).filter(this::porConfirmar);
        if (comprador.isPresent()) {
            ResultadoCodigo r = confirmarCodigo(TipoUtilizador.COMPRADOR, comprador.get().getId(), codigo);
            if (r.estado() == EstadoCodigo.CONFIRMADO) return r;
            resultado = maisInformativo(resultado, r);
        }
        return resultado;
    }

    private boolean activar(TipoUtilizador tipo, Long id) {
        return switch (tipo) {
            case PRODUTOR -> produtorRepository.findById(id).map(p -> {
                p.setEmailVerificado(true);
                produtorRepository.save(p);
                return true;
            }).orElse(false);
            case COMPRADOR -> compradorRepository.findById(id).map(c -> {
                c.setEmailVerificado(true);
                compradorRepository.save(c);
                return true;
            }).orElse(false);
            case ADMIN -> false;
        };
    }

    /** Entre dois resultados falhados, o que dá mais pistas ao utilizador (formato > errado > bloqueado > expirado). */
    private static ResultadoCodigo maisInformativo(ResultadoCodigo a, ResultadoCodigo b) {
        return peso(b.estado()) > peso(a.estado()) ? b : a;
    }

    private static int peso(EstadoCodigo e) {
        return switch (e) {
            case FORMATO_INVALIDO -> 3;
            case INCORRETO -> 2;
            case BLOQUEADO -> 1;
            case EXPIRADO, CONFIRMADO -> 0;
        };
    }
}
