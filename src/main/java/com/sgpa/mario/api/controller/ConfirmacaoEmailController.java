package com.example.demo.api.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.api.service.ConfirmacaoEmailService;
import com.example.demo.api.service.ConfirmacaoEmailService.DadosConfirmacao;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Páginas web do fluxo de confirmação de e-mail (links enviados por e-mail).
 * Template: confirmar-email.html (estados: PERGUNTAR, CONFIRMADO, CANCELADO, INVALIDO).
 */
@Controller
public class ConfirmacaoEmailController {

    private final ConfirmacaoEmailService confirmacaoEmailService;

    public ConfirmacaoEmailController(ConfirmacaoEmailService confirmacaoEmailService) {
        this.confirmacaoEmailService = confirmacaoEmailService;
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
            + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
    }

    /** Abre o link do e-mail: mostra "Foi você?" sem alterar nada. */
    @GetMapping("/confirmar-email")
    public String pagina(@RequestParam String token, Model model) {
        Optional<DadosConfirmacao> dados = confirmacaoEmailService.consultar(token);
        if (dados.isEmpty()) {
            model.addAttribute("estado", "INVALIDO");
            return "confirmar-email";
        }
        model.addAttribute("estado", "PERGUNTAR");
        model.addAttribute("token", token);
        model.addAttribute("tipo", dados.get().tipo());
        model.addAttribute("nome", dados.get().nome());
        model.addAttribute("email", dados.get().email());
        return "confirmar-email";
    }

    /** Botão "Sim, fui eu". */
    @PostMapping("/confirmar-email")
    public String confirmar(@RequestParam String token, Model model) {
        model.addAttribute("estado", confirmacaoEmailService.confirmar(token) ? "CONFIRMADO" : "INVALIDO");
        return "confirmar-email";
    }

    /** Botão "Não fui eu": cancela o registo. */
    @PostMapping("/rejeitar-conta")
    public String rejeitar(@RequestParam String token, Model model) {
        model.addAttribute("estado", confirmacaoEmailService.rejeitar(token) ? "CANCELADO" : "INVALIDO");
        return "confirmar-email";
    }

    /** Formulário da página de login: reenviar o link de confirmação. */
    @PostMapping("/reenviar-confirmacao")
    public String reenviar(@RequestParam String email, HttpServletRequest request, RedirectAttributes redirectAttrs) {
        confirmacaoEmailService.reenviarConfirmacao(email.trim(), baseUrl(request));
        redirectAttrs.addFlashAttribute("sucesso",
            "Se existir uma conta por confirmar com este e-mail, enviámos um novo link. Verifique a caixa de entrada (e o spam).");
        return "redirect:/login";
    }
}
