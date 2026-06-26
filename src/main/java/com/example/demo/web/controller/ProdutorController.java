package com.example.demo.web.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.web.config.WebMvcConfig;
import com.example.demo.web.domain.*;
import com.example.demo.web.repository.*;

@Controller
@RequestMapping("/produtor")
public class ProdutorController {

    private final OfertaRepository    ofertaRepository;
    private final PedidoRepository    pedidoRepository;
    private final ProdutorRepository  produtorRepository;
    private final BairroRepository    bairroRepository;
    private final DistritoRepository  distritoRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CompradorRepository compradorRepository;
    private final TransacaoRepository transacaoRepository;

    private static final String UPLOAD_DIR = WebMvcConfig.UPLOAD_BASE + java.io.File.separator + "images" + java.io.File.separator + "ofertas";
    private static final String UPLOAD_DIR_PERFIL = WebMvcConfig.UPLOAD_BASE + java.io.File.separator + "images" + java.io.File.separator + "perfis";

    public ProdutorController(OfertaRepository ofertaRepository,
                              PedidoRepository pedidoRepository,
                              ProdutorRepository produtorRepository,
                              BairroRepository bairroRepository,
                              DistritoRepository distritoRepository,
                              ProvinciaRepository provinciaRepository,
                              CompradorRepository compradorRepository,
                              TransacaoRepository transacaoRepository) {
        this.ofertaRepository    = ofertaRepository;
        this.pedidoRepository    = pedidoRepository;
        this.produtorRepository  = produtorRepository;
        this.bairroRepository    = bairroRepository;
        this.distritoRepository  = distritoRepository;
        this.provinciaRepository = provinciaRepository;
        this.compradorRepository = compradorRepository;
        this.transacaoRepository = transacaoRepository;
    }

    private Long getSessionId(HttpSession session) {
        Object sid = session.getAttribute("loggedInId");
        return sid != null ? ((Number) sid).longValue() : null;
    }

    // DASHBOARD
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long id,
                            HttpSession session, Model model) {
        if (id == null) id = getSessionId(session);

        Produtor produtor = null;
        if (id != null) produtor = produtorRepository.findById(id).orElse(null);

        long totalOfertas = 0, totalPedidos = 0, pedidosPendentes = 0, pedidosAceites = 0;
        List<Oferta> ultimasOfertas = new ArrayList<>();
        List<Pedido> ultimosPedidos = new ArrayList<>();

        if (produtor != null) {
            final Long prodId = produtor.getId();
            List<Oferta> minhasOfertas = ofertaRepository.ofertasPorProdutor(prodId);
            totalOfertas = minhasOfertas.size();
            ultimasOfertas = minhasOfertas.stream()
                    .sorted((a, b) -> b.getDataPublicacao().compareTo(a.getDataPublicacao()))
                    .limit(3).toList();

            List<Pedido> todosPedidos = pedidoRepository.findAll().stream()
                    .filter(p -> p.getOferta() != null &&
                                 p.getOferta().getProdutor() != null &&
                                 prodId.equals(p.getOferta().getProdutor().getId()))
                    .toList();
            totalPedidos = todosPedidos.size();
            pedidosPendentes = todosPedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.PENDENTE).count();
            pedidosAceites = todosPedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.ACEITE).count();
            ultimosPedidos = todosPedidos.stream()
                    .filter(p -> p.getDataPedido() != null &&
                                 p.getDataPedido().isAfter(LocalDateTime.now().minusDays(7)))
                    .toList();
        }

        // ─── Verifica se o produtor também é comprador (mesmo e-mail) ──────────
        Long compradorIdAssociado = null;
        if (produtor != null) {
            compradorIdAssociado = compradorRepository.findByEmail(produtor.getEmail())
                    .map(c -> c.getId())
                    .orElse(null);
        }
        boolean jaEComprador = compradorIdAssociado != null;

        model.addAttribute("produtor",           produtor);
        model.addAttribute("totalOfertas",       totalOfertas);
        model.addAttribute("totalPedidos",       totalPedidos);
        model.addAttribute("pedidosPendentes",   pedidosPendentes);
        model.addAttribute("pedidosAceites",     pedidosAceites);
        model.addAttribute("ultimasOfertas",     ultimasOfertas);
        model.addAttribute("ultimosPedidos",     ultimosPedidos);
        model.addAttribute("jaEComprador",       jaEComprador);
        model.addAttribute("compradorId",        compradorIdAssociado);
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
        if (produtor.getEmail() != null && !produtor.getEmail().isBlank()) {
            if (produtorRepository.existsByEmail(produtor.getEmail())) {
                model.addAttribute("erro", "Este e-mail já está registado. Faça login.");
                model.addAttribute("produtor", produtor);
                return "produtor/criar-produtor";
            }
        }

        if (produtor.getNuit() != null && !produtor.getNuit().isBlank()) {
            if (produtorRepository.existsByNuit(produtor.getNuit())) {
                model.addAttribute("erro", "Este NUIT já está registado por outro produtor.");
                model.addAttribute("produtor", produtor);
                return "produtor/criar-produtor";
            }
        }

        // Guardar primeiro para obter o ID
        Produtor salvo = produtorRepository.save(produtor);

        // Processar foto de perfil se enviada
        if (fotoPerfilFile != null && !fotoPerfilFile.isEmpty()) {
            try {
                String original = fotoPerfilFile.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : ".jpg";
                String filename = "produtor_" + salvo.getId() + "_" + UUID.randomUUID() + ext;
                Path uploadPath = Paths.get(UPLOAD_DIR_PERFIL);
                Files.createDirectories(uploadPath);
                java.nio.file.Files.copy(fotoPerfilFile.getInputStream(), uploadPath.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                salvo.setFotoPerfil("/uploads/images/perfis/" + filename);
                produtorRepository.save(salvo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        session.setAttribute("loggedInType", "PRODUTOR");
        session.setAttribute("loggedInId", salvo.getId());
        session.setAttribute("loggedInNome", salvo.getNomeCompleto());
        return "redirect:/produtor/dashboard?id=" + salvo.getId();
    }

    // EDITAR PERFIL
    @GetMapping("/editar-perfil")
    public String editarPerfil(@RequestParam Long id, Model model) {
        Produtor produtor = produtorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produtor não encontrado."));
        model.addAttribute("produtor", produtor);
        return "produtor/editar-perfil-produtor";
    }

    @PostMapping("/editar-perfil")
    public String salvarPerfil(@ModelAttribute Produtor produtor,
                               @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoPerfilFile,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {
        if (produtor.getEmail() != null && !produtor.getEmail().isBlank()) {
            produtorRepository.findByEmail(produtor.getEmail()).ifPresent(existente -> {
                if (!existente.getId().equals(produtor.getId()))
                    throw new RuntimeException("E-mail já registado por outro produtor.");
            });
        }

        Produtor existing = produtorRepository.findById(produtor.getId()).orElseThrow();

        // Preserve existing senha if the field was left blank
        if (produtor.getSenha() == null || produtor.getSenha().isBlank()) {
            produtor.setSenha(existing.getSenha());
        }

        // Handle foto de perfil upload
        if (fotoPerfilFile != null && !fotoPerfilFile.isEmpty()) {
            try {
                String original = fotoPerfilFile.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : ".jpg";
                String filename = "produtor_" + produtor.getId() + "_" + UUID.randomUUID() + ext;
                Path uploadPath = Paths.get(UPLOAD_DIR_PERFIL);
                Files.createDirectories(uploadPath);
                java.nio.file.Files.copy(fotoPerfilFile.getInputStream(), uploadPath.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                produtor.setFotoPerfil("/uploads/images/perfis/" + filename);
            } catch (IOException e) {
                e.printStackTrace();
                produtor.setFotoPerfil(existing.getFotoPerfil());
            }
        } else {
            produtor.setFotoPerfil(existing.getFotoPerfil());
        }

        produtorRepository.save(produtor);
        session.setAttribute("loggedInNome", produtor.getNomeCompleto());
        redirectAttrs.addFlashAttribute("sucesso", "Perfil actualizado com sucesso!");
        return "redirect:/produtor/dashboard?id=" + produtor.getId();
    }

    // PUBLICAR OFERTA
    @GetMapping("/publicar-oferta")
    public String formPublicarOferta(@RequestParam(required = false) Long produtorId,
                                     HttpSession session, Model model) {
        if (produtorId == null) produtorId = getSessionId(session);
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

        if (produtorId == null) produtorId = getSessionId(session);

        // LOCALIZAÇÃO
        if (provinciaNome != null && !provinciaNome.isBlank()) {
            Provincia provincia = provinciaRepository.findByNome(provinciaNome)
                    .orElseGet(() -> { Provincia p = new Provincia(); p.setNome(provinciaNome); return provinciaRepository.save(p); });

            if (distritoNome != null && !distritoNome.isBlank()) {
                Distrito distrito = distritoRepository.findByNomeAndProvincia(distritoNome, provincia)
                        .orElseGet(() -> { Distrito d = new Distrito(); d.setNome(distritoNome); d.setProvincia(provincia); return distritoRepository.save(d); });

                if (bairroNome != null && !bairroNome.isBlank()) {
                    Bairro bairro = bairroRepository.findByNomeAndDistrito(bairroNome, distrito)
                            .orElseGet(() -> { Bairro b = new Bairro(); b.setNome(bairroNome); b.setDistrito(distrito); return bairroRepository.save(b); });
                    oferta.setBairro(bairro);
                }
            }
        }

        // UPLOAD DE IMAGEM
        if (imagemFile != null && !imagemFile.isEmpty()) {
            try {
                String original = imagemFile.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : ".jpg";
                String filename = UUID.randomUUID() + ext;
                Path uploadPath = Paths.get(UPLOAD_DIR);
                Files.createDirectories(uploadPath);
                java.nio.file.Files.copy(imagemFile.getInputStream(), uploadPath.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                oferta.setImagemPath("/uploads/images/ofertas/" + filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // PRODUTOR
        Produtor produtor = null;
        if (produtorId != null) produtor = produtorRepository.findById(produtorId).orElse(null);
        if (produtor == null) throw new RuntimeException("Produtor não encontrado. Faça login primeiro.");

        oferta.setProdutor(produtor);
        ofertaRepository.save(oferta);
        return "redirect:/produtor/dashboard?id=" + produtor.getId();
    }

    // EDITAR OFERTA
    @GetMapping("/editar-oferta/{id}")
    public String editarOferta(@PathVariable Long id,
                               @RequestParam(required = false) Long produtorId, Model model) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada."));
        model.addAttribute("oferta", oferta);
        model.addAttribute("produtorId", produtorId != null ? produtorId : oferta.getProdutor().getId());
        return "produtor/editar-oferta";
    }

    @PostMapping("/editar-oferta/{id}")
    public String salvarEdicaoOferta(@PathVariable Long id,
                                     @ModelAttribute Oferta ofertaEditada,
                                     @RequestParam(required = false) Long produtorId,
                                     @RequestParam(value = "imagemFile", required = false) MultipartFile imagemFile) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada."));
        oferta.setNomeProduto(ofertaEditada.getNomeProduto());
        oferta.setCategoria(ofertaEditada.getCategoria());
        oferta.setPreco(ofertaEditada.getPreco());
        oferta.setQuantidadeTotal(ofertaEditada.getQuantidadeTotal());
        oferta.setQuantidadeRestante(ofertaEditada.getQuantidadeRestante());
        oferta.setUnidade(ofertaEditada.getUnidade());
        oferta.setDescricao(ofertaEditada.getDescricao());
        oferta.setEstado(ofertaEditada.getEstado());
        if (ofertaEditada.getDataValidade() != null) oferta.setDataValidade(ofertaEditada.getDataValidade());

        // ─── PROCESSAR NOVA IMAGEM SE ENVIADA ──────────────────────────────
        if (imagemFile != null && !imagemFile.isEmpty()) {
            try {
                String original = imagemFile.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : ".jpg";
                String filename = UUID.randomUUID() + ext;
                Path uploadPath = Paths.get(UPLOAD_DIR);
                Files.createDirectories(uploadPath);
                java.nio.file.Files.copy(imagemFile.getInputStream(), uploadPath.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                oferta.setImagemPath("/uploads/images/ofertas/" + filename);
            } catch (IOException e) {
                e.printStackTrace();
                // Mantém a imagem anterior em caso de erro
            }
        }

        ofertaRepository.save(oferta);
        Long redirectId = produtorId != null ? produtorId : oferta.getProdutor().getId();
        return "redirect:/produtor/minhas-ofertas?produtorId=" + redirectId;
    }

    // APAGAR OFERTA
    @PostMapping("/apagar-oferta/{id}")
    public String apagarOferta(@PathVariable Long id,
                               @RequestParam(required = false) Long produtorId) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada."));
        Long redirectId = produtorId != null ? produtorId : oferta.getProdutor().getId();
        ofertaRepository.deleteById(id);
        return "redirect:/produtor/minhas-ofertas?produtorId=" + redirectId;
    }

    // MINHAS OFERTAS
    @GetMapping("/minhas-ofertas")
    public String minhasOfertas(@RequestParam(required = false) Long produtorId,
                                HttpSession session, Model model) {
        if (produtorId == null) produtorId = getSessionId(session);
        Produtor produtor = null;
        if (produtorId != null) {
            produtor = produtorRepository.findById(produtorId).orElse(null);
            model.addAttribute("minhasOfertas", ofertaRepository.ofertasPorProdutor(produtorId));
            model.addAttribute("produtorId", produtorId);
        } else {
            model.addAttribute("minhasOfertas", ofertaRepository.findAll());
        }
        model.addAttribute("produtor", produtor);
        return "produtor/minhas-ofertas";
    }

    // PEDIDOS
    @GetMapping("/pedidos")
    public String pedidos(HttpSession session, Model model) {
        Long prodId = getSessionId(session);
        Produtor produtor = null;
        java.util.List<Pedido> pedidos;
        if (prodId != null) {
            produtor = produtorRepository.findById(prodId).orElse(null);
            pedidos = pedidoRepository.findAll().stream()
                    .filter(p -> p.getOferta() != null &&
                                 p.getOferta().getProdutor() != null &&
                                 prodId.equals(p.getOferta().getProdutor().getId()))
                    .toList();
        } else {
            pedidos = pedidoRepository.findAll();
        }
        model.addAttribute("pedidosRecebidos", pedidos);

        // Mapa pedidoId → Transacao para mostrar método/número do comprador
        java.util.Map<Long, Transacao> transacaoPorPedido = new java.util.HashMap<>();
        for (Pedido p : pedidos) {
            transacaoRepository.findByPedidoId(p.getId()).ifPresent(t -> transacaoPorPedido.put(p.getId(), t));
        }
        model.addAttribute("transacaoPorPedido", transacaoPorPedido);
        model.addAttribute("produtor", produtor);
        return "produtor/pedidos";
    }

    // HISTÓRICO
    @GetMapping("/historico")
    public String historico(HttpSession session, Model model) {
        Long prodId = getSessionId(session);
        Produtor produtor = null;
        java.util.List<com.example.demo.web.domain.Pedido> vendas;
        if (prodId != null) {
            produtor = produtorRepository.findById(prodId).orElse(null);
            vendas = pedidoRepository.findAll().stream()
                    .filter(p -> p.getOferta() != null &&
                                 p.getOferta().getProdutor() != null &&
                                 prodId.equals(p.getOferta().getProdutor().getId()))
                    .toList();
        } else {
            vendas = pedidoRepository.findAll();
        }
        model.addAttribute("historicoVendas", vendas);
        // Mapa pedidoId → Transacao para mostrar detalhes de pagamento
        java.util.Map<Long, Transacao> transacaoPorPedido = new java.util.HashMap<>();
        for (com.example.demo.web.domain.Pedido p : vendas) {
            transacaoRepository.findByPedidoId(p.getId()).ifPresent(t -> transacaoPorPedido.put(p.getId(), t));
        }
        model.addAttribute("transacaoPorPedido", transacaoPorPedido);
        model.addAttribute("produtor", produtor);
        return "produtor/historico";
    }

    // ACEITAR PEDIDO
    @PostMapping("/pedido/aceitar")
    public String aceitarPedido(@RequestParam Long pedidoId,
                                @RequestParam(required = false) String numeroPagamento,
                                @RequestParam(required = false, defaultValue = "pedidos") String from,
                                HttpSession session,
                                RedirectAttributes redirectAttrs) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado."));

        if (numeroPagamento == null || numeroPagamento.isBlank()) {
            redirectAttrs.addFlashAttribute("erro", "Por favor, indique o número para receber o pagamento antes de aceitar.");
            return "redirect:/produtor/pedidos";
        }

        // Guardar o número de pagamento e mudar estado para AGUARDANDO_PAGAMENTO
        pedido.setNumeroPagamentoProdutor(numeroPagamento.trim());
        pedido.setEstado(Pedido.EstadoPedido.AGUARDANDO_PAGAMENTO);
        pedido.setDataResposta(java.time.LocalDateTime.now());
        pedidoRepository.save(pedido);

        // A transação ainda NÃO é confirmada — só após o comprador efectuar o pagamento

        redirectAttrs.addFlashAttribute("sucesso", "Pedido aceite! O comprador vai receber o seu número para efectuar o pagamento.");
        if ("dashboard".equals(from)) {
            Long prodId = getSessionId(session);
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
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado."));
        pedido.setEstado(Pedido.EstadoPedido.REJEITADO);
        pedido.setDataResposta(java.time.LocalDateTime.now());
        if (motivoRejeicao != null && !motivoRejeicao.isBlank()) {
            pedido.setMotivoRejeicao(motivoRejeicao.trim());
        }
        pedidoRepository.save(pedido);

        // ─── CANCELAR TRANSAÇÃO ─────────────────────────────────────────────
        transacaoRepository.findByPedidoId(pedidoId).ifPresent(t -> {
            t.setEstado(Transacao.EstadoTransacao.CANCELADA);
            transacaoRepository.save(t);
        });

        // ─── RESTAURAR STOCK DA OFERTA AO REJEITAR ───────────────────────
        Oferta oferta = pedido.getOferta();
        if (oferta != null && pedido.getQuantidade() != null
                && pedido.getQuantidade().compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal restanteActual = oferta.getQuantidadeRestante() != null
                    ? oferta.getQuantidadeRestante()
                    : java.math.BigDecimal.ZERO;
            java.math.BigDecimal restaurado = restanteActual.add(pedido.getQuantidade());
            // Não ultrapassar a quantidade total original
            if (oferta.getQuantidadeTotal() != null
                    && restaurado.compareTo(oferta.getQuantidadeTotal()) > 0) {
                restaurado = oferta.getQuantidadeTotal();
            }
            oferta.setQuantidadeRestante(restaurado);
            // Reactivar a oferta se estava inactiva por falta de stock
            if (oferta.getEstado() == Oferta.EstadoOferta.INATIVO) {
                oferta.setEstado(Oferta.EstadoOferta.ATIVO);
            }
            ofertaRepository.save(oferta);
        }

        redirectAttrs.addFlashAttribute("sucesso", "Pedido rejeitado.");
        if ("dashboard".equals(from)) {
            Long prodId = getSessionId(session);
            return "redirect:/produtor/dashboard" + (prodId != null ? "?id=" + prodId : "");
        }
        return "redirect:/produtor/pedidos";
    }

    // TORNAR-SE COMPRADOR
    @GetMapping("/tornar-comprador")
    public String tornarComprador(HttpSession session, Model model) {
        Long produtorId = getSessionId(session);
        Produtor produtor = null;
        if (produtorId != null) {
            produtor = produtorRepository.findById(produtorId).orElse(null);
        }

        // Se já tem conta de comprador com o mesmo e-mail → entra directamente
        if (produtor != null) {
            var compradorExistente = compradorRepository.findByEmail(produtor.getEmail());
            if (compradorExistente.isPresent()) {
                com.example.demo.web.domain.Comprador c = compradorExistente.get();
                session.setAttribute("loggedInType", "COMPRADOR");
                session.setAttribute("loggedInId",   c.getId());
                session.setAttribute("loggedInNome", c.getNomeCompleto());
                return "redirect:/comprador/dashboard?id=" + c.getId();
            }
        }

        // Ainda não é comprador → mostra formulário pré-preenchido
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