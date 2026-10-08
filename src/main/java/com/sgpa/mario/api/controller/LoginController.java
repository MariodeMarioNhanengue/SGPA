package com.sgpa.mario.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sgpa.mario.api.service.AuthService;
import com.sgpa.mario.api.service.AuthService.ResultadoLogin;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String tipo, Model model) {
        model.addAttribute("tipo", tipo != null ? tipo : "");
        return "login";
    }

    @PostMapping("/login")
    public String processarLogin(
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam(required = false) String tipo,
            HttpSession session,
            Model model) {

        ResultadoLogin resultado = authService.autenticar(email, senha, session);

        if (resultado.sucesso()) {
            return switch (resultado.tipo()) {
                case PRODUTOR -> "redirect:/produtor/dashboard?id=" + resultado.id();
                case COMPRADOR -> "redirect:/comprador/dashboard?id=" + resultado.id();
            };
        }

        if (resultado.bloqueado()) {
            model.addAttribute("erro", "Demasiadas tentativas falhadas. Tente novamente dentro de "
                + resultado.minutosRestantes() + " minuto(s).");
            model.addAttribute("emailValue", email);
            return "login";
        }

        if (resultado.emailPorConfirmar()) {
            model.addAttribute("emailPorConfirmar", true);
            model.addAttribute("emailValue", resultado.emailNaoConfirmado());
            return "login";
        }

        model.addAttribute("erro", "Email ou senha incorrectos. Verifique os seus dados.");
        model.addAttribute("emailValue", email);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/";
    }
}
