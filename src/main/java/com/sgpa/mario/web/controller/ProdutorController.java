package com.example.demo.web.controller;

import java.math.BigDecimal;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.web.domain.*;
import com.example.demo.web.service.AuthService;
import com.example.demo.web.service.ProdutorService;
import com.example.demo.web.service.exception.NaoAutorizadoException;
import com.example.demo.web.service.exception.RegraNegocioException;

@Controller
@RequestMapping("/produtor")
public class ProdutorController {

    private final ProdutorService produtorService;
    private final AuthService authService;

    public ProdutorController(ProdutorService produtorService, AuthService authService) {
        this.produtorService = produtorService;
        this.authService = authService;
    }

    // DASHBOARD
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long id,
                            HttpSession session, Model model) {
        if (id == null) id = authService.getSessionId(session);
        var d = produtorService.dashboard(id);

        model.addAttribute("produtor",         d.produtor());
        model.addAttribute("totalOfertas",     d.totalOfertas());
        model.addAttribute("totalPedidos",     d.totalPedidos());
        model.addAttribute("pedidosPendentes", d.pedidosPendentes());
        model.addAttribute("pedidosAceites",   d.pedidosAceites());
        model.addAttribute("ultimasOfertas",   d.ultimasOfertas());
        model.addAttribute("ultimosPedidos",   d.ultimosPedidos());
        model.addAttribute("jaEComprador",     d.jaEComprador());
        model.addAttribute("compradorId",      d.compradorId());
        return "produtor/dashboard-produtor";
    }

    // CRIAR PRODUTOR
    @GetMapping("/criar")
    public String criarProdutor(Model model) {
        model.addAttribute("produtor", new Produtor());
        return "produtor/criar-produtor";
    }

    @PostMapping("/cadastrar")
    public String salvarProdutor(@ModelAttribute Produtor produtor,
                                 @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoPerfilFile,
                                 HttpSession session, Model model) {
        try {
            Produtor salvo = produtorService.cadastrar(produtor, fotoPerfilFile);
            authService.mudarSessaoParaProdutor(session, salvo);
            return "redirect:/produtor/dashboard?id=" + salvo.getId();
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("produtor", produtor);
            return "produtor/criar-produtor";
        }
    }

    // EDITAR PERFIL
    @GetMapping("/editar-perfil")
    public String editarPerfil(@RequestParam Long id, Model model) {
        model.addAttribute("produtor", produtorService.obterPorId(id));
        return "produtor/editar-perfil-produtor";
    }

    @PostMapping("/editar-perfil")
    public String salvarPerfil(@ModelAttribute Produtor produtor,
                               @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoPerfilFile,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {
        Produtor salvo = produtorService.atualizarPerfil(produtor, fotoPerfilFile);
        session.setAttribute("loggedInNome", salvo.getNomeCompleto());
        redirectAttrs.addFlashAttribute("sucesso", "Perfil actualizado com sucesso!");
        return "redirect:/produtor/dashboard?id=" + salvo.getId();
    }

    // PUBLICAR OFERTA
    @GetMapping("/publicar-oferta")
    public String formPublicarOferta(@RequestParam(required = false) Long produtorId,
                                     HttpSession session, Model model) {
        if (produtorId == null) produtorId = authService.getSessionId(session);
        model.addAttribute("oferta", new Oferta());
        model.addAttribute("produtorId", produtorId);
        return "produtor/publicar-oferta";
    }

    @PostMapping("/publicar-oferta")
    public String publicarOferta(
            @ModelAttribute Oferta oferta,
            @RequestParam(required = false) String provinciaNome,
            @RequestParam(required = false) String distritoNome,
            @RequestParam(required = false) String bairroNome,
            @RequestParam(required = false) Long produtorId,
            @RequestParam(value = "imagemFile", required = false) MultipartFile imagemFile,
            HttpSession session) {

        if (produtorId == null) produtorId = authService.getSessionId(session);
        Oferta salva = produtorService.publicarOferta(oferta, provinciaNome, distritoNome, bairroNome, produtorId, imagemFile);
        return "redirect:/produtor/dashboard?id=" + salva.getProdutor().getId();
    }

    // EDITAR OFERTA
    @GetMapping("/editar-oferta/{id}")
    public String editarOferta(@PathVariable Long id,
                               @RequestParam(required = false) Long produtorId, Model model) {
        Oferta oferta = produtorService.obterOferta(id);
        model.addAttribute("oferta", oferta);
        model.addAttribute("produtorId", produtorId != null ? produtorId : oferta.getProdutor().getId());
        return "produtor/editar-oferta";
    }

    @PostMapping("/editar-oferta/{id}")
    public String salvarEdicaoOferta(@PathVariable Long id,
                                     @ModelAttribute Oferta ofertaEditada,
                                     @RequestParam(required = false) Long produtorId,
                                     @RequestParam(value = "imagemFile", required = false) MultipartFile imagemFile,
                                     HttpSession session,
                                     RedirectAttributes redirectAttrs) {
        try {
            Oferta oferta = produtorService.editarOferta(id, ofertaEditada, produtorId, imagemFile);
            Long redirectId = produtorId != null ? produtorId : oferta.getProdutor().getId();
            return "redirect:/produtor/minhas-ofertas?produtorId=" + redirectId;
        } catch (NaoAutorizadoException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
            return "redirect:/produtor/minhas-ofertas?produtorId=" + authService.getSessionId(session);
        }
    }

    // APAGAR OFERTA
    @PostMapping("/apagar-oferta/{id}")
    public String apagarOferta(@PathVariable Long id,
                               @RequestParam(required = false) Long produtorId,
                               RedirectAttributes redirectAttrs) {
        try {
            Long redirectId = produtorService.apagarOferta(id, produtorId);
            return "redirect:/produtor/minhas-ofertas?produtorId=" + redirectId;
        } catch (NaoAutorizadoException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
            return "redirect:/produtor/minhas-ofertas?produtorId=" + produtorId;
        }
    }

    // MINHAS OFERTAS
    @GetMapping("/minhas-ofertas")
    public String minhasOfertas(@RequestParam(required = false) Long produtorId,
                                HttpSession session, Model model) {
        if (produtorId == null) produtorId = authService.getSessionId(session);
        var m = produtorService.minhasOfertas(produtorId);
        model.addAttribute("minhasOfertas", m.ofertas());
        model.addAttribute("produtor", m.produtor());
        if (produtorId != null) model.addAttribute("produtorId", produtorId);
        return "produtor/minhas-ofertas";
    }

    // PEDIDOS
    @GetMapping("/pedidos")
    public String pedidos(HttpSession session, Model model) {
        Long prodId = authService.getSessionId(session);
        var p = produtorService.pedidosRecebidos(prodId);
        model.addAttribute("pedidosRecebidos", p.pedidos());
        model.addAttribute("transacaoPorPedido", p.transacaoPorPedido());
        model.addAttribute("produtor", p.produtor());
        return "produtor/pedidos";
    }

    // HISTÓRICO
    @GetMapping("/historico")
    public String historico(HttpSession session, Model model) {
        Long prodId = authService.getSessionId(session);
        var h = produtorService.historicoVendas(prodId);
        model.addAttribute("historicoVendas", h.pedidos());
        model.addAttribute("transacaoPorPedido", h.transacaoPorPedido());
        model.addAttribute("produtor", h.produtor());
        return "produtor/historico";
    }

    // ACEITAR PEDIDO
    @PostMapping("/pedido/aceitar")
    public String aceitarPedido(@RequestParam Long pedidoId,
                                @RequestParam(required = false) String numeroPagamento,
                                @RequestParam(required = false, defaultValue = "pedidos") String from,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        Long prodId = authService.getSessionId(session);
        try {
            produtorService.aceitarPedido(pedidoId, numeroPagamento, prodId);
            redirectAttrs.addFlashAttribute("sucesso", "Pedido aceite! O comprador vai receber o seu número para efectuar o pagamento.");
        } catch (RegraNegocioException | NaoAutorizadoException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        if ("dashboard".equals(from)) {
            return "redirect:/produtor/dashboard" + (prodId != null ? "?id=" + prodId : "");
        }
        return "redirect:/produtor/pedidos";
    }

    // REJEITAR PEDIDO
    @PostMapping("/pedido/rejeitar")
    public String rejeitarPedido(@RequestParam Long pedidoId,
                                 @RequestParam(required = false, defaultValue = "pedidos") String from,
                                 @RequestParam(required = false) String motivoRejeicao,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        Long prodId = authService.getSessionId(session);
        try {
            produtorService.rejeitarPedido(pedidoId, motivoRejeicao, prodId);
            redirectAttrs.addFlashAttribute("sucesso", "Pedido cancelado.");
        } catch (NaoAutorizadoException | RegraNegocioException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        if ("dashboard".equals(from)) {
            return "redirect:/produtor/dashboard" + (prodId != null ? "?id=" + prodId : "");
        }
        return "redirect:/produtor/pedidos";
    }

    // TORNAR-SE COMPRADOR
    @GetMapping("/tornar-comprador")
    public String tornarComprador(HttpSession session, Model model) {
        Long produtorId = authService.getSessionId(session);
        Produtor produtor = null;
        if (produtorId != null) {
            try { produtor = produtorService.obterPorId(produtorId); } catch (RuntimeException ignored) {}
        }

        if (produtor != null) {
            var compradorExistente = authService.contaCompradorAssociada(produtor);
            if (compradorExistente.isPresent()) {
                Comprador c = compradorExistente.get();
                authService.mudarSessaoParaComprador(session, c);
                return "redirect:/comprador/dashboard?id=" + c.getId();
            }
        }

        if (produtor != null) {
            model.addAttribute("nomePre",     produtor.getNome());
            model.addAttribute("apelidoPre",  produtor.getApelido());
            model.addAttribute("telefonePre", produtor.getTelefone());
            model.addAttribute("emailPre",    produtor.getEmail());
        }
        model.addAttribute("modoConversao", true);
        return "comprador/criar-comprador";
    }

}
