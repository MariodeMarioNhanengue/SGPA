package com.example.demo.web.controller;



import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.web.domain.Oferta;
import com.example.demo.web.repository.OfertaRepository;
import com.example.demo.web.repository.PedidoRepository;

@Controller
@RequestMapping("/produtor")
public class ProdutorController {

    private final OfertaRepository ofertaRepository;
    private final PedidoRepository pedidoRepository;

    public ProdutorController(OfertaRepository ofertaRepository,
                              PedidoRepository pedidoRepository) {
        this.ofertaRepository = ofertaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    // ===============================
    // CRIAR CONTA
    // ===============================
    @GetMapping("/criar")
    public String abrirFormulario(Model model) {
        model.addAttribute("oferta", new Oferta());
        return "produtor/criar-produtor";
    }

    @PostMapping("/cadastrar")
    public String cadastrar() {
        return "redirect:/produtor/dashboard";
    }

    // ===============================
    // DASHBOARD
    // ===============================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // Estatísticas
        model.addAttribute("totalOfertas", ofertaRepository.count());
        model.addAttribute("totalPedidos", pedidoRepository.count());

        model.addAttribute("pedidosPendentes",
                pedidoRepository.countPendentes());

        model.addAttribute("pedidosAceites",
                pedidoRepository.countAceites());

        model.addAttribute("pedidosRejeitados",
                pedidoRepository.countRejeitados());

        // Últimas ofertas
        model.addAttribute("ultimasOfertas",
                ofertaRepository.findTop3ByOrderByDataPublicacaoDesc());

        // Pedidos recentes (últimos 7 dias)
        model.addAttribute("pedidosRecentes",
                pedidoRepository.findByDataPedidoAfter(
                        LocalDateTime.now().minusDays(7)
                )
        );

        return "produtor/dashboard-produtor";
    }

    // ===============================
    // PUBLICAR OFERTA
    // ===============================
    @GetMapping("/publicar-oferta")
    public String publicarOferta(Model model) {
        model.addAttribute("oferta", new Oferta());
        return "produtor/publicar-oferta";
    }

    @PostMapping("/publicar-oferta")
    public String salvarOferta(@ModelAttribute Oferta oferta) {

        // Evita erro de quantidadeRestante null
        if (oferta.getQuantidadeRestante() == null) {
            oferta.setQuantidadeRestante(oferta.getQuantidadeTotal());
        }

        ofertaRepository.save(oferta);

        return "redirect:/produtor/minhas-ofertas";
    }

    // ===============================
    // MINHAS OFERTAS
    // ===============================
    @GetMapping("/minhas-ofertas")
    public String minhasOfertas(Model model) {

        model.addAttribute("minhasOfertas", ofertaRepository.findAll());

        return "produtor/minhas-ofertas";
    }

    // ===============================
    // PEDIDOS
    // ===============================
    @GetMapping("/pedidos")
    public String pedidos(Model model) {

        model.addAttribute("pedidosRecebidos",
                pedidoRepository.findAll());

        return "produtor/pedidos";
    }
}