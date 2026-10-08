package com.sgpa.mario.api.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sgpa.mario.web.domain.Comprador;
import com.sgpa.mario.web.domain.Produtor;
import com.sgpa.mario.api.repository.CompradorRepository;
import com.sgpa.mario.api.repository.ProdutorRepository;

import jakarta.servlet.http.HttpSession;

/**
 * Regras de negócio de autenticação e autorização para os utilizadores
 * públicos (Produtor / Comprador). Todas as verificações de permissão
 * (quem está autenticado, com que papel, e associações entre contas)
 * vivem aqui — os controllers apenas tratam do pedido HTTP em si.
 */
@Service
public class AuthService {

    public enum TipoUtilizador { PRODUTOR, COMPRADOR }

    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final PasswordEncoderService passwordEncoderService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(ProdutorRepository produtorRepository,
                       CompradorRepository compradorRepository,
                       PasswordEncoderService passwordEncoderService,
                       LoginAttemptService loginAttemptService) {
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.passwordEncoderService = passwordEncoderService;
        this.loginAttemptService = loginAttemptService;
    }

    /** Resultado de uma tentativa de autenticação. */
    public record ResultadoLogin(boolean sucesso, TipoUtilizador tipo, Long id,
                                 boolean bloqueado, long minutosRestantes,
                                 boolean emailPorConfirmar, String emailNaoConfirmado) {
        public static ResultadoLogin falha() {
            return new ResultadoLogin(false, null, null, false, 0, false, null);
        }
        public static ResultadoLogin bloqueado(long minutos) {
            return new ResultadoLogin(false, null, null, true, minutos, false, null);
        }
        public static ResultadoLogin emailPorConfirmar(String email) {
            return new ResultadoLogin(false, null, null, false, 0, true, email);
        }
        public static ResultadoLogin ok(TipoUtilizador tipo, Long id) {
            return new ResultadoLogin(true, tipo, id, false, 0, false, null);
        }
    }

    /**
     * Valida as credenciais e, em caso de sucesso, estabelece a sessão do utilizador.
     * Contas com e-mail por validar ENTRAM, mas em modo restrito: o ContaVerificadaInterceptor
     * bloqueia qualquer acção até a conta ser validada (pelo e-mail ou por um administrador).
     */
    public ResultadoLogin autenticar(String email, String senha, HttpSession session) {
        if (loginAttemptService.estaBloqueado(email)) {
            return ResultadoLogin.bloqueado(loginAttemptService.minutosRestantes(email));
        }

        Optional<Produtor> produtor = produtorRepository.findByEmail(email);
        if (produtor.isPresent() && senhaCorreta(produtor.get().getSenha(), senha)) {
            Produtor p = produtor.get();
            if (!p.isContaAtiva()) {
                return ResultadoLogin.falha();
            }
            loginAttemptService.registarSucesso(email);
            migrarSenhaSeNecessario(p, senha);
            p.setUltimoAcesso(LocalDateTime.now());
            produtorRepository.save(p);
            iniciarSessao(session, TipoUtilizador.PRODUTOR, p.getId(), p.getNomeCompleto());
            return ResultadoLogin.ok(TipoUtilizador.PRODUTOR, p.getId());
        }

        Optional<Comprador> comprador = compradorRepository.findByEmail(email);
        if (comprador.isPresent() && senhaCorreta(comprador.get().getSenha(), senha)) {
            Comprador c = comprador.get();
            if (!c.isContaAtiva()) {
                return ResultadoLogin.falha();
            }
            loginAttemptService.registarSucesso(email);
            migrarSenhaSeNecessario(c, senha);
            c.setUltimoAcesso(LocalDateTime.now());
            compradorRepository.save(c);
            iniciarSessao(session, TipoUtilizador.COMPRADOR, c.getId(), c.getNomeCompleto());
            return ResultadoLogin.ok(TipoUtilizador.COMPRADOR, c.getId());
        }

        loginAttemptService.registarFalha(email);
        return ResultadoLogin.falha();
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private boolean senhaCorreta(String senhaGuardada, String senhaFornecida) {
        return passwordEncoderService.matches(senhaFornecida, senhaGuardada);
    }

    /** Se a conta ainda tiver a senha em texto simples, substitui-a pelo hash agora que sabemos que está correcta. */
    private void migrarSenhaSeNecessario(Produtor p, String senhaEmTextoSimples) {
        if (!passwordEncoderService.isHash(p.getSenha())) {
            p.setSenha(passwordEncoderService.encode(senhaEmTextoSimples));
        }
    }

    private void migrarSenhaSeNecessario(Comprador c, String senhaEmTextoSimples) {
        if (!passwordEncoderService.isHash(c.getSenha())) {
            c.setSenha(passwordEncoderService.encode(senhaEmTextoSimples));
        }
    }

    private void iniciarSessao(HttpSession session, TipoUtilizador tipo, Long id, String nome) {
        session.setAttribute("loggedInType", tipo.name());
        session.setAttribute("loggedInId", id);
        session.setAttribute("loggedInNome", nome);
    }

    // ─── Conta por validar ───────────────────────────────────────────────

    /** Conta de sessão cujo e-mail ainda não foi validado. */
    public record ContaPorVerificar(TipoUtilizador tipo, Long id, String email) {}

    /**
     * Se o utilizador autenticado na sessão tiver a conta por validar devolve-a, senão vazio.
     * Consulta sempre a base de dados: assim, quando o utilizador confirma o e-mail (ou o admin
     * valida a conta) as restrições levantam-se de imediato, sem precisar de novo login.
     * Só {@code emailVerificado == false} conta como por validar (null = contas antigas, já activas).
     */
    public Optional<ContaPorVerificar> contaPorVerificar(HttpSession session) {
        if (session == null) return Optional.empty();
        String tipo = getSessionTipo(session);
        Long id = getSessionId(session);
        if (tipo == null || id == null) return Optional.empty();

        if ("PRODUTOR".equals(tipo)) {
            return produtorRepository.findById(id)
                .filter(p -> Boolean.FALSE.equals(p.getEmailVerificado()))
                .map(p -> new ContaPorVerificar(TipoUtilizador.PRODUTOR, p.getId(), p.getEmail()));
        }
        if ("COMPRADOR".equals(tipo)) {
            return compradorRepository.findById(id)
                .filter(c -> Boolean.FALSE.equals(c.getEmailVerificado()))
                .map(c -> new ContaPorVerificar(TipoUtilizador.COMPRADOR, c.getId(), c.getEmail()));
        }
        return Optional.empty();
    }

    /**
     * true se a sessão pertence a uma conta (Produtor ou Comprador) com este MESMO e-mail já validado.
     * Usado na conversão de conta: quem já provou ser dono do e-mail não precisa de o validar outra vez.
     * Só vale para o e-mail da própria sessão, por isso ninguém consegue "herdar" a validação de outro e-mail.
     */
    public boolean emailJaVerificadoNaSessao(HttpSession session, String email) {
        if (session == null || email == null) return false;
        String tipo = getSessionTipo(session);
        Long id = getSessionId(session);
        if (tipo == null || id == null) return false;
        String alvo = email.trim();
        if ("PRODUTOR".equals(tipo)) {
            return produtorRepository.findById(id)
                .filter(p -> p.getEmail() != null && p.getEmail().equalsIgnoreCase(alvo))
                .map(p -> !Boolean.FALSE.equals(p.getEmailVerificado())).orElse(false);
        }
        if ("COMPRADOR".equals(tipo)) {
            return compradorRepository.findById(id)
                .filter(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(alvo))
                .map(c -> !Boolean.FALSE.equals(c.getEmailVerificado())).orElse(false);
        }
        return false;
    }

    // ─── Consulta de identidade / permissões ────────────────────────────

    public Long getSessionId(HttpSession session) {
        Object sid = session.getAttribute("loggedInId");
        return sid != null ? ((Number) sid).longValue() : null;
    }

    public String getSessionTipo(HttpSession session) {
        return (String) session.getAttribute("loggedInType");
    }

    public boolean isProdutorNaSessao(Long produtorId, HttpSession session) {
        return "PRODUTOR".equals(getSessionTipo(session)) && produtorId.equals(getSessionId(session));
    }

    // ─── Regras de conversão de conta (Produtor ⇄ Comprador) ────────────

    /** Devolve a conta de Comprador associada ao mesmo e-mail deste Produtor, se existir. */
    public Optional<Comprador> contaCompradorAssociada(Produtor produtor) {
        return compradorRepository.findByEmail(produtor.getEmail());
    }

    /** Devolve a conta de Produtor associada ao mesmo e-mail deste Comprador, se existir. */
    public Optional<Produtor> contaProdutorAssociada(Comprador comprador) {
        return produtorRepository.findByEmail(comprador.getEmail());
    }

    /** Troca a sessão activa para a conta de Comprador indicada (usado na conversão de conta). */
    public void mudarSessaoParaComprador(HttpSession session, Comprador c) {
        iniciarSessao(session, TipoUtilizador.COMPRADOR, c.getId(), c.getNomeCompleto());
    }

    /** Troca a sessão activa para a conta de Produtor indicada (usado na conversão de conta). */
    public void mudarSessaoParaProdutor(HttpSession session, Produtor p) {
        iniciarSessao(session, TipoUtilizador.PRODUTOR, p.getId(), p.getNomeCompleto());
    }
}
