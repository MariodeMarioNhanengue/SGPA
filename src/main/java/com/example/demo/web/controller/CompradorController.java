package com.example.demo.web.controller;

import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/comprador")
public class CompradorController {

    // 🔹 Abrir formulário de criação
    @GetMapping("/criar")
    public String abrirFormulario() {
        return "comprador/criar-comprador";
    }

    // 🔹 Submeter formulário
    @PostMapping("/cadastrar")
    public String cadastrar() {
        // Aqui podes salvar no banco depois

        return "redirect:/comprador/dashboard";
    }

    // 🔹 Dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // 🔹 Evitar erro do Thymeleaf
        model.addAttribute("comprador", null);

        // 🔹 Listas vazias (para não dar erro no HTML)
        model.addAttribute("ofertas", new ArrayList<>());
        model.addAttribute("meusPedidos", new ArrayList<>());
        model.addAttribute("favoritos", new ArrayList<>());

        // 🔹 Mensagem de sucesso
        model.addAttribute("sucessoCadastro", "Conta criada com sucesso!");

        return "comprador/dashboard-comprador";
    }
}
