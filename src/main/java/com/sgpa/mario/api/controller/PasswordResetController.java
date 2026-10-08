package com.sgpa.mario.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sgpa.mario.api.service.PasswordResetService;
import com.sgpa.mario.api.service.exception.RegraNegocioException;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
            + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
    }

    // ─── PEDIR RECUPERAÇÃO ──────────────────────────────────────────────

    @GetMapping("/recuperar-senha")
    public String formularioRecuperar() {
        return "recuperar-senha";
    }

    @PostMapping("/recuperar-senha")
    public String processarRecuperar(@RequestParam String email,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttrs) {
        passwordResetService.solicitarRecuperacao(email, baseUrl(request));
        redirectAttrs.addFlashAttribute("sucesso",
            "Se existir uma conta registada com este email, enviámos um link de recuperação. Verifique a sua caixa de entrada (e o spam).");
        return "redirect:/login";
    }

    // ─── DEFINIR NOVA SENHA ─────────────────────────────────────────────

    @GetMapping("/redefinir-senha")
    public String formularioRedefinir(@RequestParam String token, Model model) {
        boolean valido = passwordResetService.validarToken(token).isPresent();
        model.addAttribute("tokenValido", valido);
        model.addAttribute("token", token);
        return "redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    public String processarRedefinir(@RequestParam String token,
                                     @RequestParam String novaSenha,
                                     @RequestParam String confirmarSenha,
                                     Model model,
                                     RedirectAttributes redirectAttrs) {
        try {
            passwordResetService.redefinirSenha(token, novaSenha, confirmarSenha);
            redirectAttrs.addFlashAttribute("sucesso", "Senha redefinida com sucesso. Já pode entrar com a nova senha.");
            return "redirect:/login";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("tokenValido", passwordResetService.validarToken(token).isPresent());
            model.addAttribute("token", token);
            return "redefinir-senha";
        }
    }
}
