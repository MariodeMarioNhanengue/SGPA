package com.sgpa.mario.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sgpa.mario.api.service.AdminAuthService;
import com.sgpa.mario.api.service.AuthService;
import com.sgpa.mario.api.service.AuthService.ResultadoLogin;

import jakarta.servlet.http.HttpSession;

/**
 * Login e logout do site, nos mesmos URLs da API (/api/v1/auth/...):
 * formulário -> site com sessão (aqui); application/json -> API com JWT (AuthApiController).
 */
@Controller
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final AuthService authService;
    private final AdminAuthService adminAuthService;

    public LoginController(AuthService authService, AdminAuthService adminAuthService) {
        this.authService = authService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String tipo, Model model) {
        model.addAttribute("tipo", tipo != null ? tipo : "");
        return "login";
    }

    @PostMapping(value = "/login", consumes = {"application/x-www-form-urlencoded", "multipart/form-data"})
    public String processarLogin(
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam(required = false) String tipo,
            HttpSession session,
            Model model) {

        ResultadoLogin resultado = authService.autenticar(email, senha, session);

        if (resultado.sucesso()) {
            return switch (resultado.tipo()) {
                case PRODUTOR -> "redirect:/api/v1/produtor/dashboard?id=" + resultado.id();
                case COMPRADOR -> "redirect:/api/v1/comprador/dashboard?id=" + resultado.id();
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

    // ─── Administradores ─────────────────────────────────────────────────

    @GetMapping("/admin/login")
    public String loginAdminPage() { return "admin/login-admin"; }

    @PostMapping(value = "/admin/login", consumes = {"application/x-www-form-urlencoded", "multipart/form-data"})
    public String processarLoginAdmin(@RequestParam String email,
                                      @RequestParam String senha,
                                      HttpSession session, Model model) {
        AdminAuthService.ResultadoLoginAdmin resultado = adminAuthService.autenticar(email, senha, session);

        if (resultado.sucesso()) {
            return "redirect:/api/v1/admin/dashboard";
        }
        if (resultado.bloqueado()) {
            model.addAttribute("erro", "Demasiadas tentativas falhadas. Tente novamente dentro de "
                + resultado.minutosRestantes() + " minuto(s).");
        } else {
            model.addAttribute("erro", "Credenciais inválidas. Verifique o e-mail e a senha.");
        }
        return "admin/login-admin";
    }

    @GetMapping("/admin/logout")
    public String logoutAdmin(HttpSession session) {
        adminAuthService.logout(session);
        return "redirect:/api/v1/auth/admin/login";
    }
}
