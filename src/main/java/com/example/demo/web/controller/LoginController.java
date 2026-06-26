package com.example.demo.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.repository.CompradorRepository;
import com.example.demo.web.repository.ProdutorRepository;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Controller
public class LoginController {

    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;

    public LoginController(ProdutorRepository produtorRepository,
                           CompradorRepository compradorRepository) {
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
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

        // Tentar login como Produtor
        var produtor = produtorRepository.findByEmail(email);
        if (produtor.isPresent()) {
            Produtor p = produtor.get();
            if (p.getSenha() != null && p.getSenha().equals(senha)) {
                p.setUltimoAcesso(LocalDateTime.now());
                produtorRepository.save(p);
                session.setAttribute("loggedInType", "PRODUTOR");
                session.setAttribute("loggedInId", p.getId());
                session.setAttribute("loggedInNome", p.getNomeCompleto());
                return "redirect:/produtor/dashboard?id=" + p.getId();
            }
        }

        // Tentar login como Comprador
        var comprador = compradorRepository.findByEmail(email);
        if (comprador.isPresent()) {
            Comprador c = comprador.get();
            if (c.getSenha() != null && c.getSenha().equals(senha)) {
                c.setUltimoAcesso(LocalDateTime.now());
                compradorRepository.save(c);
                session.setAttribute("loggedInType", "COMPRADOR");
                session.setAttribute("loggedInId", c.getId());
                session.setAttribute("loggedInNome", c.getNomeCompleto());
                return "redirect:/comprador/dashboard?id=" + c.getId();
            }
        }

        model.addAttribute("erro", "Email ou senha incorrectos. Verifique os seus dados.");
        model.addAttribute("emailValue", email);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}