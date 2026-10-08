package com.sgpa.mario.api.config;

import java.util.Optional;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import com.sgpa.mario.api.service.AuthService;
import com.sgpa.mario.api.service.AuthService.ContaPorVerificar;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Disponibiliza a todas as páginas MVC se o utilizador da sessão tem a conta por validar,
 * para o aviso (banner + lembrete de 30 em 30 minutos) aparecer sem cada controller ter de o saber.
 */
@ControllerAdvice
public class ContaPorVerificarAdvice {

    private final AuthService authService;

    public ContaPorVerificarAdvice(AuthService authService) {
        this.authService = authService;
    }

    @ModelAttribute
    public void contaPorVerificar(HttpServletRequest request, Model model) {
        // getSession(false): nunca cria sessões (a API REST /api/** é stateless)
        HttpSession session = request.getSession(false);
        Optional<ContaPorVerificar> conta = authService.contaPorVerificar(session);
        model.addAttribute("contaPorVerificar", conta.isPresent());
        conta.ifPresent(c -> model.addAttribute("contaPorVerificarEmail", c.email()));
    }
}
