package com.sgpa.mario.api.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sgpa.mario.api.service.AuthService;
import com.sgpa.mario.api.service.AuthService.ContaPorVerificar;
import com.sgpa.mario.api.service.ConfirmacaoEmailService;
import com.sgpa.mario.api.service.TokenVerificacaoService.EstadoCodigo;
import com.sgpa.mario.api.service.TokenVerificacaoService.ResultadoCodigo;
import com.sgpa.mario.web.domain.TokenVerificacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Páginas web da confirmação de e-mail por código de 6 dígitos.
 *
 * O código é enviado por e-mail no registo; o utilizador (que já tem sessão, em modo restrito) escreve-o
 * em /api/v1/auth/confirmar-email. Se estiver certo a conta fica activa de imediato e vai para o seu painel.
 * Template: confirmar-codigo.html
 */
@Controller
@RequestMapping("/api/v1/auth")
public class ConfirmacaoEmailController {

    private final ConfirmacaoEmailService confirmacaoEmailService;
    private final AuthService authService;

    public ConfirmacaoEmailController(ConfirmacaoEmailService confirmacaoEmailService, AuthService authService) {
        this.confirmacaoEmailService = confirmacaoEmailService;
        this.authService = authService;
    }

    /** Página onde se escreve o código. Só faz sentido para quem tem a conta por validar na sessão. */
    @GetMapping("/confirmar-email")
    public String pagina(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false); // não cria sessão para visitantes anónimos
        Optional<ContaPorVerificar> conta = authService.contaPorVerificar(session);
        if (conta.isEmpty()) {
            return "redirect:" + destinoSemContaPorVerificar(session);
        }
        preencher(model, conta.get());
        return "confirmar-codigo";
    }

    /** Botão "Confirmar": código certo -> conta activa e entra directamente no painel. */
    @PostMapping(value = "/confirmar-email", consumes = {"application/x-www-form-urlencoded", "multipart/form-data"})
    public String confirmar(@RequestParam(defaultValue = "") String codigo, HttpServletRequest request,
                            Model model, RedirectAttributes redirectAttrs) {
        HttpSession session = request.getSession(false);
        Optional<ContaPorVerificar> conta = authService.contaPorVerificar(session);
        if (conta.isEmpty()) {
            return "redirect:" + destinoSemContaPorVerificar(session);
        }
        ContaPorVerificar c = conta.get();

        ResultadoCodigo resultado = confirmacaoEmailService.confirmarCodigo(
            TokenVerificacao.TipoUtilizador.valueOf(c.tipo().name()), c.id(), codigo);

        if (resultado.estado() == EstadoCodigo.CONFIRMADO) {
            redirectAttrs.addFlashAttribute("sucesso",
                "E-mail confirmado! A sua conta está activa e já pode usar a plataforma.");
            return "redirect:" + painel(c);
        }
        model.addAttribute("erro", mensagem(resultado));
        preencher(model, c);
        return "confirmar-codigo";
    }

    /**
     * Pede um código novo (o anterior deixa de valer). Usado na página do código, no aviso do painel e na página
     * de login. A resposta é sempre a mesma, exista ou não a conta: não revela que e-mails estão registados.
     */
    @PostMapping(value = "/reenviar-confirmacao", consumes = {"application/x-www-form-urlencoded", "multipart/form-data"})
    public String reenviar(@RequestParam String email, HttpServletRequest request,
                           RedirectAttributes redirectAttrs) {
        confirmacaoEmailService.reenviarConfirmacao(email.trim());
        redirectAttrs.addFlashAttribute("sucesso",
            "Se existir uma conta por confirmar com este e-mail, enviámos um novo código. "
            + "Verifique a caixa de entrada (e o spam). Só pode pedir um novo código de minuto a minuto.");

        HttpSession session = request.getSession(false);
        if (authService.contaPorVerificar(session).isPresent()) {
            return "redirect:/api/v1/auth/confirmar-email";
        }
        return "redirect:" + destinoSemContaPorVerificar(session);
    }

    // ─── Auxiliares ──────────────────────────────────────────────────────

    private void preencher(Model model, ContaPorVerificar conta) {
        model.addAttribute("email", conta.email());
        model.addAttribute("tipo", conta.tipo().name());
        model.addAttribute("painelUrl", painel(conta));
        model.addAttribute("validadeMinutos", ConfirmacaoEmailService.VALIDADE_CODIGO.toMinutes());
    }

    private String painel(ContaPorVerificar conta) {
        return (conta.tipo() == AuthService.TipoUtilizador.PRODUTOR ? "/api/v1/produtor" : "/api/v1/comprador")
            + "/dashboard?id=" + conta.id();
    }

    /** Sem conta por validar: quem já está num painel volta lá; os restantes vão ao login. */
    private String destinoSemContaPorVerificar(HttpSession session) {
        Object tipo = session == null ? null : session.getAttribute("loggedInType");
        if ("PRODUTOR".equals(tipo)) return "/api/v1/produtor/dashboard";
        if ("COMPRADOR".equals(tipo)) return "/api/v1/comprador/dashboard";
        return "/api/v1/auth/login";
    }

    private String mensagem(ResultadoCodigo r) {
        return switch (r.estado()) {
            case INCORRETO -> "Código incorrecto. Ainda tem " + r.tentativasRestantes()
                + (r.tentativasRestantes() == 1 ? " tentativa." : " tentativas.");
            case BLOQUEADO -> "Demasiadas tentativas falhadas: este código foi anulado. Peça um novo código.";
            case EXPIRADO -> "Este código expirou ou já não é válido. Peça um novo código.";
            case FORMATO_INVALIDO -> "Introduza os 6 dígitos do código.";
            case CONFIRMADO -> "";
        };
    }
}
