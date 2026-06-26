package com.example.demo.web.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

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
@RequestMapping("/comprador")
public class CompradorController {

    private final CompradorRepository compradorRepository;
    private final BairroRepository bairroRepository;
    private final DistritoRepository distritoRepository;
    private final ProvinciaRepository provinciaRepository;
    private final OfertaRepository ofertaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProdutorRepository produtorRepository;
    private final TransacaoRepository transacaoRepository;


    private static final String UPLOAD_DIR_PERFIL = WebMvcConfig.UPLOAD_BASE + java.io.File.separator + "images" + java.io.File.separator + "perfis";

 public CompradorController(CompradorRepository compradorRepository,
                           BairroRepository bairroRepository,
                           DistritoRepository distritoRepository,
                           ProvinciaRepository provinciaRepository,
                           OfertaRepository ofertaRepository,
                           PedidoRepository pedidoRepository,
                           ProdutorRepository produtorRepository,
                           TransacaoRepository transacaoRepository
                           ) {
    this.compradorRepository  = compradorRepository;
    this.bairroRepository     = bairroRepository;
    this.distritoRepository   = distritoRepository;
    this.provinciaRepository  = provinciaRepository;
    this.ofertaRepository     = ofertaRepository;
    this.pedidoRepository     = pedidoRepository;
    this.produtorRepository   = produtorRepository;
    this.transacaoRepository  = transacaoRepository;
}

    private Long getSessionId(HttpSession session) {
        Object sid = session.getAttribute("loggedInId");
        return sid != null ? ((Number) sid).longValue() : null;
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

        if (compradorRepository.existsByEmail(comprador.getEmail())) {
            model.addAttribute("erro", "Este e-mail já está registado. Faça login.");
            return "comprador/criar-comprador";
        }

        // Normalizar NUIT: string vazia → null (evita violação de UNIQUE)
        if (comprador.getNuit() != null && comprador.getNuit().isBlank()) {
            comprador.setNuit(null);
        }

        if (comprador.getNuit() != null) {
            if (compradorRepository.existsByNuit(comprador.getNuit())) {
                model.addAttribute("erro", "Este NUIT já está registado por outro comprador.");
                return "comprador/criar-comprador";
            }
        }

        resolverLocalizacao(comprador, provinciaNome, distritoNome, bairroNome);

        // Guardar primeiro para obter o ID
        Comprador salvo = compradorRepository.save(comprador);

        // Processar foto de perfil se enviada
        if (fotoPerfilFile != null && !fotoPerfilFile.isEmpty()) {
            try {
                String original = fotoPerfilFile.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : ".jpg";
                String filename = "comprador_" + salvo.getId() + "_" + UUID.randomUUID() + ext;
                Path uploadPath = Paths.get(UPLOAD_DIR_PERFIL);
                Files.createDirectories(uploadPath);
                java.nio.file.Files.copy(fotoPerfilFile.getInputStream(), uploadPath.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                salvo.setFotoPerfil("/uploads/images/perfis/" + filename);
                compradorRepository.save(salvo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        session.setAttribute("loggedInType", "COMPRADOR");
        session.setAttribute("loggedInId",   salvo.getId());
        session.setAttribute("loggedInNome", salvo.getNomeCompleto());
        return "redirect:/comprador/dashboard?id=" + salvo.getId();
    }

    // ─── DASHBOARD ─────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long id,
                            @RequestParam(required = false) String nome,
                            @RequestParam(required = false) String categoria,
                            @RequestParam(required = false) String dataInicio,
                            @RequestParam(required = false) String dataFim,
                            HttpSession session, Model model) {

        if (id == null) id = getSessionId(session);

        Comprador comprador = null;
        if (id != null) comprador = compradorRepository.findById(id).orElse(null);

        // Converter strings de data para LocalDateTime
        java.time.LocalDateTime dtInicio = null;
        java.time.LocalDateTime dtFim    = null;
        if (dataInicio != null && !dataInicio.isBlank()) {
            try { dtInicio = java.time.LocalDate.parse(dataInicio).atStartOfDay(); } catch (Exception ignored) {}
        }
        if (dataFim != null && !dataFim.isBlank()) {
            try { dtFim = java.time.LocalDate.parse(dataFim).atTime(23, 59, 59); } catch (Exception ignored) {}
        }
        boolean temFiltroDatas = dtInicio != null || dtFim != null;

        List<Oferta> ofertas;
        boolean temFiltro = (nome != null && !nome.trim().isEmpty()) ||
                            (categoria != null && !categoria.trim().isEmpty());

        if (temFiltro || temFiltroDatas) {
            // Parte por texto/categoria (igual ao anterior)
            List<Oferta> base;
            if (temFiltro) {
                if (nome != null && !nome.trim().isEmpty() &&
                    categoria != null && !categoria.trim().isEmpty()) {
                    final String nomeFinal = nome.trim().toLowerCase();
                    final String catFinal  = categoria.trim();
                    base = ofertaRepository.findAll().stream()
                            .filter(o -> o.getNomeProduto() != null &&
                                         o.getNomeProduto().toLowerCase().contains(nomeFinal) &&
                                         o.getCategoria() != null &&
                                         o.getCategoria().equalsIgnoreCase(catFinal))
                            .toList();
                } else if (nome != null && !nome.trim().isEmpty()) {
                    base = ofertaRepository.findByNomeProdutoContainingIgnoreCase(nome.trim());
                } else {
                    base = ofertaRepository.findByCategoria(categoria.trim());
                }
            } else {
                base = ofertaRepository.findByEstado(Oferta.EstadoOferta.ATIVO);
            }

            // Aplicar filtro de datas em cima do resultado
            if (temFiltroDatas) {
                final java.time.LocalDateTime fInicio = dtInicio;
                final java.time.LocalDateTime fFim    = dtFim;
                base = base.stream()
                        .filter(o -> o.getDataPublicacao() != null &&
                                     (fInicio == null || !o.getDataPublicacao().isBefore(fInicio)) &&
                                     (fFim    == null || !o.getDataPublicacao().isAfter(fFim)))
                        .toList();
            }
            ofertas = base;
        } else {
            ofertas = ofertaRepository.findByEstado(Oferta.EstadoOferta.ATIVO);
        }

        List<Pedido> meusPedidos = new ArrayList<>();
        if (comprador != null) {
            meusPedidos = pedidoRepository.findByCompradorId(comprador.getId());
        }

        // ─── Verifica se o utilizador também é produtor ─────────────────────────
        // Usa a sessão (mais fiável) ou pesquisa por e-mail como fallback
        Long produtorDoCompradorId = null;
        String sessionType = (String) session.getAttribute("loggedInType");
        Long sessionId     = getSessionId(session);

        if ("PRODUTOR".equals(sessionType) && sessionId != null) {
            // Utilizador está autenticado como produtor → usa o ID da sessão
            produtorDoCompradorId = sessionId;
        } else if (comprador != null) {
            // Verifica por e-mail como fallback
            produtorDoCompradorId = produtorRepository.findByEmail(comprador.getEmail())
                    .map(p -> p.getId())
                    .orElse(null);
        }

        boolean jaEProdutor = produtorDoCompradorId != null;

        model.addAttribute("comprador",            comprador);
        model.addAttribute("ofertas",              ofertas);
        model.addAttribute("meusPedidos",          meusPedidos);
        model.addAttribute("favoritos",            new ArrayList<>());
        model.addAttribute("produtorDoCompradorId", produtorDoCompradorId);
        model.addAttribute("produtorId",           produtorDoCompradorId);
        model.addAttribute("jaEProdutor",          jaEProdutor);
        return "comprador/dashboard-comprador";
    }

    // ─── PÁGINA DE PEDIDOS DO COMPRADOR ────────────────────────────
    @GetMapping("/pedidos")
    public String meusPedidos(@RequestParam(required = false) Long id,
                              HttpSession session, Model model) {
        if (id == null) id = getSessionId(session);

        Comprador comprador = null;
        List<Pedido> meusPedidos = new ArrayList<>();

        if (id != null) {
            comprador = compradorRepository.findById(id).orElse(null);
            meusPedidos = pedidoRepository.findByCompradorId(id);
        }

        model.addAttribute("comprador",   comprador);
        model.addAttribute("meusPedidos", meusPedidos);
        return "comprador/pedidos-comprador";
    }

    // ─── FAZER PEDIDO ───────────────────────────────────────────────
    @PostMapping("/comprar")
    public String comprar(@RequestParam Long ofertaId,
                          @RequestParam(required = false) Long compradorId,
                          @RequestParam(required = false) java.math.BigDecimal quantidade,
                          HttpSession session,
                          RedirectAttributes redirectAttrs) {
        if (compradorId == null) compradorId = getSessionId(session);

        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada."));
        Comprador comprador = compradorRepository.findById(compradorId)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado."));

        // ─── Impede que o produtor compre o seu próprio produto ─────────────────
        if (oferta.getProdutor() != null) {
            Long ofertaProdutorId = oferta.getProdutor().getId();

            // Verificação 1: sessão está como PRODUTOR e o ID coincide (mais fiável)
            String sType = (String) session.getAttribute("loggedInType");
            Long   sId   = getSessionId(session);
            boolean porSessao = "PRODUTOR".equals(sType) && ofertaProdutorId.equals(sId);

            // Verificação 2: e-mail do comprador coincide com o do produtor da oferta
            boolean porEmail = produtorRepository.findByEmail(comprador.getEmail())
                    .map(p -> p.getId().equals(ofertaProdutorId))
                    .orElse(false);

            if (porSessao || porEmail) {
                redirectAttrs.addFlashAttribute("erro",
                        "Não é possível encomendar o seu próprio produto. " +
                        "Esta oferta foi publicada por si enquanto produtor.");
                return "redirect:/comprador/dashboard?id=" + compradorId;
            }
        }

        if (oferta.getEstado() != Oferta.EstadoOferta.ATIVO) {
            redirectAttrs.addFlashAttribute("erro", "Esta oferta já não está disponível.");
            return "redirect:/comprador/dashboard?id=" + compradorId;
        }

        if (quantidade != null && quantidade.compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal restante = oferta.getQuantidadeRestante();
            if (restante == null || quantidade.compareTo(restante) > 0) {
                redirectAttrs.addFlashAttribute("erro",
                    "Quantidade solicitada (" + quantidade + " " + oferta.getUnidade() + ") " +
                    "excede o disponível (" + (restante != null ? restante : 0) + " " + oferta.getUnidade() + ").");
                return "redirect:/comprador/dashboard?id=" + compradorId;
            }
        }

        Pedido pedido = new Pedido();
        pedido.setOferta(oferta);
        pedido.setComprador(comprador);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setEstado(Pedido.EstadoPedido.PENDENTE);
        if (quantidade != null && quantidade.compareTo(java.math.BigDecimal.ZERO) > 0) {
            pedido.setQuantidade(quantidade);
        }
        pedidoRepository.save(pedido);

        // ─── Redirecionar para dashboard — pagamento só após o produtor aceitar ──
        redirectAttrs.addFlashAttribute("sucesso",
                "Pedido enviado ao produtor! Aguarde a resposta para effectuar o pagamento.");
        return "redirect:/comprador/dashboard?id=" + compradorId;
    }

    // ─── PÁGINA DE PAGAMENTO ────────────────────────────────────────
    @GetMapping("/pagamento")
    public String mostrarPagamento(@RequestParam Long pedidoId,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttrs) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElse(null);
        if (pedido == null) {
            redirectAttrs.addFlashAttribute("erro", "Pedido não encontrado.");
            return "redirect:/comprador/dashboard";
        }
        // Calcular total
        BigDecimal total = BigDecimal.ZERO;
        if (pedido.getQuantidade() != null && pedido.getOferta() != null
                && pedido.getOferta().getPreco() != null) {
            total = pedido.getQuantidade().multiply(pedido.getOferta().getPreco());
        }
        model.addAttribute("pedido", pedido);
        model.addAttribute("total", total);
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
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado."));

        // Calcular total
        BigDecimal total = BigDecimal.ZERO;
        if (pedido.getQuantidade() != null && pedido.getOferta() != null
                && pedido.getOferta().getPreco() != null) {
            total = pedido.getQuantidade().multiply(pedido.getOferta().getPreco());
        }

        // Gravar transação como CONFIRMADA (o comprador já efectuou o pagamento)
        Transacao transacao = new Transacao();
        transacao.setPedido(pedido);
        transacao.setMetodoPagamento(Transacao.MetodoPagamento.valueOf(metodoPagamento));
        transacao.setOperadora(Transacao.Operadora.valueOf(operadora));
        transacao.setReferencia(referencia != null ? referencia.trim() : null);
        transacao.setValorTotal(total);
        transacao.setEstado(Transacao.EstadoTransacao.CONFIRMADA);
        transacao.setDataConfirmacao(LocalDateTime.now());
        transacaoRepository.save(transacao);

        // Mudar estado do pedido para ACEITE
        pedido.setEstado(Pedido.EstadoPedido.ACEITE);
        pedidoRepository.save(pedido);

        // ─── DEDUZIR STOCK DA OFERTA ────────────────────────────────────────
        Oferta oferta = pedido.getOferta();
        if (oferta != null && pedido.getQuantidade() != null
                && pedido.getQuantidade().compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal restanteActual = oferta.getQuantidadeRestante() != null
                    ? oferta.getQuantidadeRestante()
                    : java.math.BigDecimal.ZERO;
            java.math.BigDecimal novoRestante = restanteActual.subtract(pedido.getQuantidade());
            if (novoRestante.compareTo(java.math.BigDecimal.ZERO) < 0) {
                novoRestante = java.math.BigDecimal.ZERO;
            }
            oferta.setQuantidadeRestante(novoRestante);
            if (novoRestante.compareTo(java.math.BigDecimal.ZERO) == 0) {
                oferta.setEstado(com.example.demo.web.domain.Oferta.EstadoOferta.INATIVO);
            }
            ofertaRepository.save(oferta);
        }

        Long compradorId = pedido.getComprador() != null ? pedido.getComprador().getId() : getSessionId(session);

        redirectAttrs.addFlashAttribute("sucesso",
                "Pagamento efectuado com sucesso! O seu pedido foi concluído.");
        return "redirect:/comprador/dashboard?id=" + compradorId;
    }

    // ─── HISTÓRICO ──────────────────────────────────────────────────
    @GetMapping("/historico")
    public String historico(HttpSession session, Model model) {
        Long compradorId = getSessionId(session);
        List<Pedido> historico = new ArrayList<>();
        Comprador comprador = null;
        if (compradorId != null) {
            comprador = compradorRepository.findById(compradorId).orElse(null);
            historico = pedidoRepository.findByCompradorId(compradorId);
        } else {
            historico = pedidoRepository.findAll();
        }
        model.addAttribute("comprador", comprador);
        model.addAttribute("historicoPedidos", historico);
        return "comprador/historico-comprador";
    }

    // ─── EDITAR PERFIL – GET ────────────────────────────────────────
    @GetMapping("/editar-perfil")
    public String editarPerfil(@RequestParam Long id, Model model) {
        Comprador comprador = compradorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado."));
        model.addAttribute("comprador", comprador);
        return "comprador/editar-perfil-comprador";
    }

    // ─── EDITAR PERFIL – POST (CORRIGIDO) ───────────────────────────
    // Usa parâmetros individuais em vez de @ModelAttribute para evitar que
    // o JPA substitua relações (pedidos, bairros) por valores nulos ao salvar.
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

        // Carrega a entidade gerida pelo JPA (preserva relações como pedidos)
        Comprador comprador = compradorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado."));

        // Valida unicidade de e-mail
        compradorRepository.findByEmail(email).ifPresent(existente -> {
            if (!existente.getId().equals(id))
                throw new RuntimeException("E-mail já registado por outro comprador.");
        });

        // Actualiza campos simples
        comprador.setNome(nome);
        comprador.setApelido(apelido);
        comprador.setTelefone(telefone);
        comprador.setEmail(email);

        // Só actualiza a senha se for fornecida
        if (senha != null && !senha.isBlank()) {
            comprador.setSenha(senha);
        }

        // Actualiza foto de perfil se enviada
        if (fotoPerfilFile != null && !fotoPerfilFile.isEmpty()) {
            try {
                String original = fotoPerfilFile.getOriginalFilename();
                String ext = (original != null && original.contains("."))
                        ? original.substring(original.lastIndexOf("."))
                        : ".jpg";
                String filename = "comprador_" + id + "_" + UUID.randomUUID() + ext;
                Path uploadPath = Paths.get(UPLOAD_DIR_PERFIL);
                Files.createDirectories(uploadPath);
                java.nio.file.Files.copy(fotoPerfilFile.getInputStream(), uploadPath.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                comprador.setFotoPerfil("/uploads/images/perfis/" + filename);
            } catch (IOException e) {
                e.printStackTrace();
                // mantém foto anterior (não faz nada pois comprador já tem o valor)
            }
        }

        // Actualiza localização apenas se o utilizador escolheu novos valores;
        // caso contrário o bairro anterior é mantido (comprador.getBairros() permanece).
        if (provinciaNome != null && !provinciaNome.isBlank()) {
            resolverLocalizacao(comprador, provinciaNome, distritoNome, bairroNome);
        }

        compradorRepository.save(comprador);
        redirectAttrs.addFlashAttribute("sucesso", "Perfil actualizado com sucesso!");
        return "redirect:/comprador/dashboard?id=" + id;
    }

    // ─── HELPER – resolve e associa localização ─────────────────────
    private void resolverLocalizacao(Comprador comprador,
                                     String provinciaNome,
                                     String distritoNome,
                                     String bairroNome) {
        if (provinciaNome == null || provinciaNome.isBlank()) return;

        Provincia provincia = provinciaRepository.findByNome(provinciaNome)
                .orElseGet(() -> {
                    Provincia p = new Provincia();
                    p.setNome(provinciaNome);
                    return provinciaRepository.save(p);
                });

        if (distritoNome == null || distritoNome.isBlank()) return;

        Distrito distrito = distritoRepository.findByNomeAndProvincia(distritoNome, provincia)
                .orElseGet(() -> {
                    Distrito d = new Distrito();
                    d.setNome(distritoNome);
                    d.setProvincia(provincia);
                    return distritoRepository.save(d);
                });

        if (bairroNome == null || bairroNome.isBlank()) return;

        Bairro bairro = bairroRepository.findByNomeAndDistrito(bairroNome, distrito)
                .orElseGet(() -> {
                    Bairro b = new Bairro();
                    b.setNome(bairroNome);
                    b.setDistrito(distrito);
                    return bairroRepository.save(b);
                });

        comprador.setBairros(bairro);
    }

    // ─── CANCELAR PEDIDO (pelo comprador) ──────────────────────────
    @PostMapping("/cancelar-pedido/{pedidoId}")
    public String cancelarPedido(@PathVariable Long pedidoId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttrs) {
        Long compradorId = getSessionId(session);

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado."));

        // Garante que o pedido pertence ao comprador autenticado
        if (compradorId == null ||
            pedido.getComprador() == null ||
            !pedido.getComprador().getId().equals(compradorId)) {
            redirectAttrs.addFlashAttribute("erro", "Não tem permissão para cancelar este pedido.");
            return "redirect:/comprador/pedidos";
        }

        // Só pedidos PENDENTES podem ser cancelados
        if (pedido.getEstado() != Pedido.EstadoPedido.PENDENTE) {
            redirectAttrs.addFlashAttribute("erro",
                    "Não é possível cancelar um pedido que já foi " +
                    pedido.getEstado().name().toLowerCase() + ".");
            return "redirect:/comprador/pedidos";
        }

        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
        pedido.setDataResposta(LocalDateTime.now());
        pedidoRepository.save(pedido);

        redirectAttrs.addFlashAttribute("sucesso", "Pedido cancelado com sucesso.");
        return "redirect:/comprador/pedidos";
    }

    // ─── TORNAR-SE PRODUTOR ─────────────────────────────────────────
    @GetMapping("/tornar-produtor")
    public String tornarProdutor(HttpSession session, Model model) {
        Long compradorId = getSessionId(session);
        Comprador comprador = null;
        if (compradorId != null) {
            comprador = compradorRepository.findById(compradorId).orElse(null);
        }

        // Se já tem conta de produtor com o mesmo e-mail → entra directamente
        if (comprador != null) {
            var produtorExistente = produtorRepository.findByEmail(comprador.getEmail());
            if (produtorExistente.isPresent()) {
                com.example.demo.web.domain.Produtor p = produtorExistente.get();
                session.setAttribute("loggedInType", "PRODUTOR");
                session.setAttribute("loggedInId",   p.getId());
                session.setAttribute("loggedInNome", p.getNomeCompleto());
                return "redirect:/produtor/dashboard?id=" + p.getId();
            }
        }

        // Ainda não é produtor → mostra formulário de registo pré-preenchido
        if (comprador != null) {
            model.addAttribute("nomePre",     comprador.getNome());
            model.addAttribute("apelidoPre",  comprador.getApelido());
            model.addAttribute("telefonePre", comprador.getTelefone());
            model.addAttribute("emailPre",    comprador.getEmail());
        }
        model.addAttribute("produtor", new com.example.demo.web.domain.Produtor());
        model.addAttribute("modoConversao", true);
        return "produtor/criar-produtor";
    }
  }
