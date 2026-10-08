package com.example.demo.web.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.service.AuthService;
import com.example.demo.web.service.CompradorService;
import com.example.demo.web.service.exception.NaoAutorizadoException;
import com.example.demo.web.service.exception.RegraNegocioException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/comprador")
public class CompradorController {

    private final CompradorService compradorService;
    private final AuthService authService;

    public CompradorController(CompradorService compradorService, AuthService authService) {
        this.compradorService = compradorService;
        this.authService = authService;
    }

    // ─── ABRIR FORMULÁRIO DE CRIAÇÃO ───────────────────────────────
    @GetMapping("/criar")
    public String abrirFormulario() {
        return "comprador/criar-comprador";
    }

    // ─── CADASTRAR COMPRADOR ────────────────────────────────────────
    @PostMapping("/cadastrar")
    public String cadastrar(
            @ModelAttribute Comprador comprador,
            @RequestParam(required = false) String provinciaNome,
            @RequestParam(required = false) String distritoNome,
            @RequestParam(required = false) String bairroNome,
            @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoPerfilFile,
            HttpSession session, Model model) {

        try {
            Comprador salvo = compradorService.cadastrar(comprador, provinciaNome, distritoNome, bairroNome, fotoPerfilFile);
            authService.mudarSessaoParaComprador(session, salvo);
            return "redirect:/comprador/dashboard?id=" + salvo.getId();
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "comprador/criar-comprador";
        }
    }

    // ─── DASHBOARD ─────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long id,
                            @RequestParam(required = false) String nome,
                            @RequestParam(required = false) String categoria,
                            @RequestParam(required = false) String dataInicio,
                            @RequestParam(required = false) String dataFim,
                            HttpSession session, Model model) {

        if (id == null) id = authService.getSessionId(session);

        var d = compradorService.dashboard(id, nome, categoria, dataInicio, dataFim,
            authService.getSessionTipo(session), authService.getSessionId(session));

        model.addAttribute("comprador",             d.comprador());
        model.addAttribute("ofertas",                d.ofertas());
        model.addAttribute("meusPedidos",            d.meusPedidos());
        model.addAttribute("favoritos",              new java.util.ArrayList<>());
        model.addAttribute("produtorDoCompradorId",  d.produtorDoCompradorId());
        model.addAttribute("produtorId",             d.produtorDoCompradorId());
        model.addAttribute("jaEProdutor",            d.jaEProdutor());
        return "comprador/dashboard-comprador";
    }

    // ─── PÁGINA DE PEDIDOS DO COMPRADOR ────────────────────────────
    @GetMapping("/pedidos")
    public String meusPedidos(@RequestParam(required = false) Long id,
                              HttpSession session, Model model) {
        if (id == null) id = authService.getSessionId(session);
        var p = compradorService.meusPedidos(id);
        model.addAttribute("comprador",   p.comprador());
        model.addAttribute("meusPedidos", p.pedidos());
        model.addAttribute("transacaoPorPedido", p.transacaoPorPedido());
        return "comprador/pedidos-comprador";
    }

    // ─── FAZER PEDIDO ───────────────────────────────────────────────
    @PostMapping("/comprar")
    public String comprar(@RequestParam Long ofertaId,
                          @RequestParam(required = false) Long compradorId,
                          @RequestParam(required = false) BigDecimal quantidade,
                          HttpSession session,
                          RedirectAttributes redirectAttrs) {
        if (compradorId == null) compradorId = authService.getSessionId(session);
        try {
            compradorService.comprar(ofertaId, compradorId, quantidade,
                authService.getSessionTipo(session), authService.getSessionId(session));
            redirectAttrs.addFlashAttribute("sucesso",
                "Pedido enviado ao produtor! Aguarde a resposta para effectuar o pagamento.");
        } catch (RegraNegocioException | NaoAutorizadoException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/comprador/dashboard?id=" + compradorId;
    }

    // ─── PÁGINA DE PAGAMENTO ────────────────────────────────────────
    @GetMapping("/pagamento")
    public String mostrarPagamento(@RequestParam Long pedidoId,
                                   Model model,
                                   RedirectAttributes redirectAttrs) {
        var detalhe = compradorService.detalhePagamento(pedidoId);
        if (detalhe == null) {
            redirectAttrs.addFlashAttribute("erro", "Pedido não encontrado.");
            return "redirect:/comprador/dashboard";
        }
        model.addAttribute("pedido", detalhe.pedido());
        model.addAttribute("total", detalhe.total());
        return "comprador/pagamento";
    }

    // ─── SUBMETER PAGAMENTO ─────────────────────────────────────────
    @PostMapping("/pagamento/enviar")
    public String enviarPagamento(@RequestParam Long pedidoId,
                                  @RequestParam String metodoPagamento,
                                  @RequestParam String operadora,
                                  @RequestParam(required = false) String referencia,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs) {
        Long compradorId = compradorService.enviarPagamento(pedidoId, metodoPagamento, operadora, referencia);
        if (compradorId == null) compradorId = authService.getSessionId(session);

        redirectAttrs.addFlashAttribute("sucesso",
                "Pagamento efectuado com sucesso! O seu pedido foi concluído.");
        return "redirect:/comprador/dashboard?id=" + compradorId;
    }

    // ─── HISTÓRICO ──────────────────────────────────────────────────
    @GetMapping("/historico")
    public String historico(HttpSession session, Model model) {
        Long compradorId = authService.getSessionId(session);
        var h = compradorService.historico(compradorId);
        model.addAttribute("comprador", h.comprador());
        model.addAttribute("historicoPedidos", h.pedidos());
        return "comprador/historico-comprador";
    }

    // ─── EDITAR PERFIL – GET ────────────────────────────────────────
    @GetMapping("/editar-perfil")
    public String editarPerfil(@RequestParam Long id, Model model) {
        model.addAttribute("comprador", compradorService.obterPorId(id));
        return "comprador/editar-perfil-comprador";
    }

    // ─── EDITAR PERFIL – POST ────────────────────────────────────────
    @PostMapping("/editar-perfil")
    public String salvarPerfil(
            @RequestParam Long id,
            @RequestParam String nome,
            @RequestParam String apelido,
            @RequestParam String telefone,
            @RequestParam String email,
            @RequestParam(required = false) String senha,
            @RequestParam(required = false) String provinciaNome,
            @RequestParam(required = false) String distritoNome,
            @RequestParam(required = false) String bairroNome,
            @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoPerfilFile,
            RedirectAttributes redirectAttrs) {

        compradorService.atualizarPerfil(id, nome, apelido, telefone, email, senha,
            provinciaNome, distritoNome, bairroNome, fotoPerfilFile);
        redirectAttrs.addFlashAttribute("sucesso", "Perfil actualizado com sucesso!");
        return "redirect:/comprador/dashboard?id=" + id;
    }

    // ─── CANCELAR PEDIDO (pelo comprador) ──────────────────────────
    @PostMapping("/cancelar-pedido/{pedidoId}")
    public String cancelarPedido(@PathVariable Long pedidoId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        try {
            compradorService.cancelarPedido(pedidoId, authService.getSessionId(session));
            redirectAttrs.addFlashAttribute("sucesso", "Pedido cancelado com sucesso.");
        } catch (RegraNegocioException | NaoAutorizadoException e) {
            redirectAttrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/comprador/pedidos";
    }

    // ─── TORNAR-SE PRODUTOR ─────────────────────────────────────────
    @GetMapping("/tornar-produtor")
    public String tornarProdutor(HttpSession session, Model model) {
        Long compradorId = authService.getSessionId(session);
        Comprador comprador = null;
        if (compradorId != null) {
            try { comprador = compradorService.obterPorId(compradorId); } catch (RuntimeException ignored) {}
        }

        if (comprador != null) {
            var produtorExistente = authService.contaProdutorAssociada(comprador);
            if (produtorExistente.isPresent()) {
                Produtor p = produtorExistente.get();
                authService.mudarSessaoParaProdutor(session, p);
                return "redirect:/produtor/dashboard?id=" + p.getId();
            }
        }

        if (comprador != null) {
            model.addAttribute("nomePre",     comprador.getNome());
            model.addAttribute("apelidoPre",  comprador.getApelido());
            model.addAttribute("telefonePre", comprador.getTelefone());
            model.addAttribute("emailPre",    comprador.getEmail());
        }
        model.addAttribute("produtor", new Produtor());
        model.addAttribute("modoConversao", true);
        return "produtor/criar-produtor";
    }
  }
