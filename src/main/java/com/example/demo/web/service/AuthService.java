package com.example.demo.web.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.repository.CompradorRepository;
import com.example.demo.web.repository.ProdutorRepository;

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

    /** Valida as credenciais e, em caso de sucesso, estabelece a sessão do utilizador. */
    public ResultadoLogin autenticar(String email, String senha, HttpSession session) {
        if (loginAttemptService.estaBloqueado(email)) {
            return ResultadoLogin.bloqueado(loginAttemptService.minutosRestantes(email));
        }

        Optional<Produtor> produtor = produtorRepository.findByEmail(email);
        if (produtor.isPresent() && senhaCorreta(produtor.get().getSenha(), senha)) {
            Produtor p = produtor.get();
            if (Boolean.FALSE.equals(p.getEmailVerificado())) {
                return ResultadoLogin.emailPorConfirmar(email);
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
            if (Boolean.FALSE.equals(c.getEmailVerificado())) {
                return ResultadoLogin.emailPorConfirmar(email);
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
