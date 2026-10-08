package com.example.demo.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Comprador;
import com.example.demo.web.domain.Oferta;
import com.example.demo.web.domain.Pedido;
import com.example.demo.web.domain.Produtor;
import com.example.demo.web.domain.Transacao;
import com.example.demo.api.repository.CompradorRepository;
import com.example.demo.api.repository.OfertaRepository;
import com.example.demo.api.repository.PedidoRepository;
import com.example.demo.api.repository.ProdutorRepository;
import com.example.demo.api.repository.TransacaoRepository;
import com.example.demo.api.service.exception.NaoAutorizadoException;
import com.example.demo.api.service.exception.RecursoNaoEncontradoException;
import com.example.demo.api.service.exception.RegraNegocioException;

/**
 * Regras de negócio da área do Comprador: cadastro, procura de ofertas,
 * pedidos, pagamento e conversão de conta. As verificações de permissão
 * (ex: um pedido só pode ser cancelado pelo seu dono, um produtor não pode
 * comprar o seu próprio produto) vivem aqui.
 */
@Service
public class CompradorService {

    private final CompradorRepository compradorRepository;
    private final OfertaRepository ofertaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProdutorRepository produtorRepository;
    private final TransacaoRepository transacaoRepository;
    private final LocalizacaoService localizacaoService;
    private final ArmazenamentoService armazenamentoService;
    private final EmailService emailService;
    private final PasswordEncoderService passwordEncoderService;
    private final ConfirmacaoEmailService confirmacaoEmailService;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    public CompradorService(CompradorRepository compradorRepository,
                            OfertaRepository ofertaRepository,
                            PedidoRepository pedidoRepository,
                            ProdutorRepository produtorRepository,
                            TransacaoRepository transacaoRepository,
                            LocalizacaoService localizacaoService,
                            ArmazenamentoService armazenamentoService,
                            EmailService emailService,
                            PasswordEncoderService passwordEncoderService,
                            ConfirmacaoEmailService confirmacaoEmailService) {
        this.compradorRepository = compradorRepository;
        this.ofertaRepository = ofertaRepository;
        this.pedidoRepository = pedidoRepository;
        this.produtorRepository = produtorRepository;
        this.transacaoRepository = transacaoRepository;
        this.localizacaoService = localizacaoService;
        this.armazenamentoService = armazenamentoService;
        this.emailService = emailService;
        this.passwordEncoderService = passwordEncoderService;
        this.confirmacaoEmailService = confirmacaoEmailService;
    }

    // ─── CADASTRO ────────────────────────────────────────────────────────

    public Comprador cadastrar(Comprador comprador, String provinciaNome, String distritoNome,
                               String bairroNome, MultipartFile fotoPerfilFile) {
        if (compradorRepository.existsByEmail(comprador.getEmail())) {
            throw new RegraNegocioException("Este e-mail já está registado. Faça login.");
        }

        if (comprador.getNuit() != null && comprador.getNuit().isBlank()) {
            comprador.setNuit(null);
        }
        if (comprador.getNuit() != null && compradorRepository.existsByNuit(comprador.getNuit())) {
            throw new RegraNegocioException("Este NUIT já está registado por outro comprador.");
        }

        Bairro bairro = localizacaoService.resolverOuCriarBairro(provinciaNome, distritoNome, bairroNome);
        if (bairro != null) comprador.setBairros(bairro);

        comprador.setSenha(passwordEncoderService.encode(comprador.getSenha()));
        comprador.setEmailVerificado(false);

        Comprador salvo = compradorRepository.save(comprador);

        String foto = armazenamentoService.guardarFotoPerfil(fotoPerfilFile, "comprador", salvo.getId());
        if (foto != null) {
            salvo.setFotoPerfil(foto);
            compradorRepository.save(salvo);
        }
        confirmacaoEmailService.enviarConfirmacaoComprador(salvo, baseUrl);
        return salvo;
    }

    // ─── DASHBOARD / PROCURA DE OFERTAS ─────────────────────────────────

    public record DashboardComprador(Comprador comprador, List<Oferta> ofertas, List<Pedido> meusPedidos,
                                     Long produtorDoCompradorId, boolean jaEProdutor) {}

    public DashboardComprador dashboard(Long compradorId, String nome, String categoria,
                                        String dataInicio, String dataFim,
                                        String sessionTipo, Long sessionId) {
        Comprador comprador = compradorId != null ? compradorRepository.findById(compradorId).orElse(null) : null;

        LocalDateTime dtInicio = parseInicioDia(dataInicio);
        LocalDateTime dtFim = parseFimDia(dataFim);
        boolean temFiltroDatas = dtInicio != null || dtFim != null;
        boolean temFiltro = (nome != null && !nome.trim().isEmpty()) || (categoria != null && !categoria.trim().isEmpty());

        List<Oferta> ofertas;
        if (temFiltro || temFiltroDatas) {
            List<Oferta> base;
            if (temFiltro) {
                if (nome != null && !nome.trim().isEmpty() && categoria != null && !categoria.trim().isEmpty()) {
                    String nomeFinal = nome.trim().toLowerCase();
                    String catFinal = categoria.trim();
                    base = ofertaRepository.findAll().stream()
                        .filter(o -> o.getNomeProduto() != null && o.getNomeProduto().toLowerCase().contains(nomeFinal)
                                  && o.getCategoria() != null && o.getCategoria().equalsIgnoreCase(catFinal))
                        .toList();
                } else if (nome != null && !nome.trim().isEmpty()) {
                    base = ofertaRepository.findByNomeProdutoContainingIgnoreCase(nome.trim());
                } else {
                    base = ofertaRepository.findByCategoria(categoria.trim());
                }
            } else {
                base = ofertaRepository.findByEstado(Oferta.EstadoOferta.ATIVO);
            }

            if (temFiltroDatas) {
                LocalDateTime fInicio = dtInicio;
                LocalDateTime fFim = dtFim;
                base = base.stream()
                    .filter(o -> o.getDataPublicacao() != null
                              && (fInicio == null || !o.getDataPublicacao().isBefore(fInicio))
                              && (fFim == null || !o.getDataPublicacao().isAfter(fFim)))
                    .toList();
            }
            ofertas = base;
        } else {
            ofertas = ofertaRepository.findByEstado(Oferta.EstadoOferta.ATIVO);
        }

        List<Pedido> meusPedidos = comprador != null
            ? pedidoRepository.findByCompradorId(comprador.getId()) : new ArrayList<>();

        Long produtorDoCompradorId = "PRODUTOR".equals(sessionTipo) && sessionId != null
            ? sessionId
            : (comprador != null
                ? produtorRepository.findByEmail(comprador.getEmail()).map(Produtor::getId).orElse(null)
                : null);

        return new DashboardComprador(comprador, ofertas, meusPedidos, produtorDoCompradorId, produtorDoCompradorId != null);
    }

    private LocalDateTime parseInicioDia(String data) {
        if (data == null || data.isBlank()) return null;
        try { return LocalDate.parse(data).atStartOfDay(); } catch (Exception e) { return null; }
    }

    private LocalDateTime parseFimDia(String data) {
        if (data == null || data.isBlank()) return null;
        try { return LocalDate.parse(data).atTime(23, 59, 59); } catch (Exception e) { return null; }
    }

    // ─── PEDIDOS DO COMPRADOR ────────────────────────────────────────────

    public record PedidosComprador(Comprador comprador, List<Pedido> pedidos, Map<Long, Transacao> transacaoPorPedido) {}

    public PedidosComprador meusPedidos(Long compradorId) {
        if (compradorId == null) return new PedidosComprador(null, new ArrayList<>(), new HashMap<>());
        Comprador comprador = compradorRepository.findById(compradorId).orElse(null);
        List<Pedido> pedidos = pedidoRepository.findByCompradorId(compradorId);
        Map<Long, Transacao> transacaoPorPedido = new HashMap<>();
        for (Pedido p : pedidos) {
            transacaoRepository.findByPedidoId(p.getId()).ifPresent(t -> transacaoPorPedido.put(p.getId(), t));
        }
        return new PedidosComprador(comprador, pedidos, transacaoPorPedido);
    }

    public PedidosComprador historico(Long compradorId) {
        if (compradorId == null) return new PedidosComprador(null, pedidoRepository.findAll(), new HashMap<>());
        Comprador comprador = compradorRepository.findById(compradorId).orElse(null);
        List<Pedido> pedidos = pedidoRepository.findByCompradorId(compradorId);
        Map<Long, Transacao> transacaoPorPedido = new HashMap<>();
        for (Pedido p : pedidos) {
            transacaoRepository.findByPedidoId(p.getId()).ifPresent(t -> transacaoPorPedido.put(p.getId(), t));
        }
        return new PedidosComprador(comprador, pedidos, transacaoPorPedido);
    }

    // ─── COMPRAR ─────────────────────────────────────────────────────────

    /** Cria um pedido de compra, validando que o comprador não é o dono da oferta e que há stock suficiente. */
    public Pedido comprar(Long ofertaId, Long compradorId, BigDecimal quantidade, String sessionTipo, Long sessionId) {
        Oferta oferta = ofertaRepository.findById(ofertaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Oferta não encontrada."));
        Comprador comprador = compradorRepository.findById(compradorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comprador não encontrado."));

        if (oferta.getProdutor() != null) {
            Long ofertaProdutorId = oferta.getProdutor().getId();
            boolean porSessao = "PRODUTOR".equals(sessionTipo) && ofertaProdutorId.equals(sessionId);
            boolean porEmail = produtorRepository.findByEmail(comprador.getEmail())
                .map(p -> p.getId().equals(ofertaProdutorId)).orElse(false);

            if (porSessao || porEmail) {
                throw new NaoAutorizadoException(
                    "Não é possível encomendar o seu próprio produto. Esta oferta foi publicada por si enquanto produtor.");
            }
        }

        if (oferta.getEstado() != Oferta.EstadoOferta.ATIVO) {
            throw new RegraNegocioException("Esta oferta já não está disponível.");
        }

        if (quantidade != null && quantidade.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal restante = oferta.getQuantidadeRestante();
            if (restante == null || quantidade.compareTo(restante) > 0) {
                throw new RegraNegocioException(
                    "Quantidade solicitada (" + quantidade + " " + oferta.getUnidade() + ") "
                    + "excede o disponível (" + (restante != null ? restante : 0) + " " + oferta.getUnidade() + ").");
            }
        }

        Pedido pedido = new Pedido();
        pedido.setOferta(oferta);
        pedido.setComprador(comprador);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setEstado(Pedido.EstadoPedido.PENDENTE);
        if (quantidade != null && quantidade.compareTo(BigDecimal.ZERO) > 0) {
            pedido.setQuantidade(quantidade);
        }
        Pedido salvo = pedidoRepository.save(pedido);

        if (oferta.getProdutor() != null) {
            String assunto = "Novo pedido recebido — " + oferta.getNomeProduto();
            String corpo = "Olá " + oferta.getProdutor().getNome() + ",\n\n"
                + "Recebeu um novo pedido no SGPA:\n\n"
                + "Produto: " + oferta.getNomeProduto() + "\n"
                + "Quantidade: " + (salvo.getQuantidade() != null ? salvo.getQuantidade() : "—") + " " + oferta.getUnidade() + "\n"
                + "Comprador: " + comprador.getNomeCompleto() + "\n"
                + "Contacto do comprador: " + (comprador.getTelefone() != null ? comprador.getTelefone() : "—") + "\n\n"
                + "Aceda à sua conta para aceitar ou rejeitar este pedido.";
            emailService.enviar(oferta.getProdutor().getEmail(), assunto, corpo);
        }

        return salvo;
    }

    // ─── PAGAMENTO ───────────────────────────────────────────────────────

    public record DetalhePagamento(Pedido pedido, BigDecimal total) {}

    public DetalhePagamento detalhePagamento(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null) return null;
        return new DetalhePagamento(pedido, calcularTotal(pedido));
    }

    private BigDecimal calcularTotal(Pedido pedido) {
        if (pedido.getQuantidade() != null && pedido.getOferta() != null && pedido.getOferta().getPreco() != null) {
            return pedido.getQuantidade().multiply(pedido.getOferta().getPreco());
        }
        return BigDecimal.ZERO;
    }

    /** Regista o pagamento de um pedido, confirma a transação e deduz o stock da oferta. */
    public Long enviarPagamento(Long pedidoId, String metodoPagamento, String operadora, String referencia) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));

        BigDecimal total = calcularTotal(pedido);

        Transacao transacao = new Transacao();
        transacao.setPedido(pedido);
        transacao.setMetodoPagamento(Transacao.MetodoPagamento.valueOf(metodoPagamento));
        transacao.setOperadora(Transacao.Operadora.valueOf(operadora));
        transacao.setReferencia(referencia != null ? referencia.trim() : null);
        transacao.setValorTotal(total);
        transacao.setEstado(Transacao.EstadoTransacao.CONFIRMADA);
        transacao.setDataConfirmacao(LocalDateTime.now());
        transacaoRepository.save(transacao);

        pedido.setEstado(Pedido.EstadoPedido.ACEITE);
        pedidoRepository.save(pedido);

        Oferta oferta = pedido.getOferta();
        if (oferta != null && pedido.getQuantidade() != null
                && pedido.getQuantidade().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal restanteActual = oferta.getQuantidadeRestante() != null
                ? oferta.getQuantidadeRestante() : BigDecimal.ZERO;
            BigDecimal novoRestante = restanteActual.subtract(pedido.getQuantidade());
            if (novoRestante.compareTo(BigDecimal.ZERO) < 0) novoRestante = BigDecimal.ZERO;
            oferta.setQuantidadeRestante(novoRestante);
            if (novoRestante.compareTo(BigDecimal.ZERO) == 0) {
                oferta.setEstado(Oferta.EstadoOferta.INATIVO);
            }
            ofertaRepository.save(oferta);
        }

        return pedido.getComprador() != null ? pedido.getComprador().getId() : null;
    }

    // ─── PERFIL ──────────────────────────────────────────────────────────

    public Comprador obterPorId(Long id) {
        return compradorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Comprador não encontrado."));
    }

    public Comprador atualizarPerfil(Long id, String nome, String apelido, String telefone, String email,
                                     String senha, String provinciaNome, String distritoNome, String bairroNome,
                                     MultipartFile fotoPerfilFile) {
        Comprador comprador = obterPorId(id);

        compradorRepository.findByEmail(email).ifPresent(existente -> {
            if (!existente.getId().equals(id))
                throw new RegraNegocioException("E-mail já registado por outro comprador.");
        });

        comprador.setNome(nome);
        comprador.setApelido(apelido);
        comprador.setTelefone(telefone);
        comprador.setEmail(email);

        if (senha != null && !senha.isBlank()) {
            comprador.setSenha(passwordEncoderService.encode(senha));
        }

        String novaFoto = armazenamentoService.guardarFotoPerfil(fotoPerfilFile, "comprador", id);
        if (novaFoto != null) comprador.setFotoPerfil(novaFoto);

        if (provinciaNome != null && !provinciaNome.isBlank()) {
            Bairro bairro = localizacaoService.resolverOuCriarBairro(provinciaNome, distritoNome, bairroNome);
            if (bairro != null) comprador.setBairros(bairro);
        }

        return compradorRepository.save(comprador);
    }

    // ─── CANCELAR PEDIDO ─────────────────────────────────────────────────

    /** Cancela um pedido pendente, garantindo que pertence ao comprador autenticado. */
    public void cancelarPedido(Long pedidoId, Long compradorId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));

        if (compradorId == null || pedido.getComprador() == null
                || !pedido.getComprador().getId().equals(compradorId)) {
            throw new NaoAutorizadoException("Não tem permissão para cancelar este pedido.");
        }

        if (pedido.getEstado() != Pedido.EstadoPedido.PENDENTE) {
            throw new RegraNegocioException(
                "Não é possível cancelar um pedido que já foi " + pedido.getEstado().name().toLowerCase() + ".");
        }

        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
        pedido.setDataResposta(LocalDateTime.now());
        pedido.setMotivoCancelamento(null);
        pedidoRepository.save(pedido);

        if (pedido.getOferta() != null && pedido.getOferta().getProdutor() != null) {
            Produtor produtorNotificar = pedido.getOferta().getProdutor();
            String assunto = "Pedido cancelado pelo comprador — " + pedido.getOferta().getNomeProduto();
            String corpo = "Olá " + produtorNotificar.getNome() + ",\n\n"
                + "O comprador " + pedido.getComprador().getNomeCompleto() + " cancelou o seguinte pedido:\n\n"
                + "Produto: " + pedido.getOferta().getNomeProduto() + "\n";
            emailService.enviar(produtorNotificar.getEmail(), assunto, corpo);
        }
    }
}
