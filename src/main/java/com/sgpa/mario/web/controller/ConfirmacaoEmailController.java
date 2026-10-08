package com.example.demo.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.web.service.ConfirmacaoEmailService;

import jakarta.servlet.http.HttpServletRequest;

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

    @GetMapping("/confirmar-email")
    public String confirmar(@RequestParam String token, Model model) {
        boolean sucesso = confirmacaoEmailService.confirmar(token);
        model.addAttribute("sucesso", sucesso);
        return "confirmar-email";
    }

    @PostMapping("/reenviar-confirmacao")
    public String reenviar(@RequestParam String email,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttrs) {
        confirmacaoEmailService.reenviarConfirmacao(email, baseUrl(request));
        redirectAttrs.addFlashAttribute("sucesso",
            "Se existir uma conta por confirmar com este email, reenviámos o link de confirmação.");
        return "redirect:/login";
    }
}
