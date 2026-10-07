package com.example.demo.api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.web.domain.Bairro;
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
 * Regras de negócio da área do Produtor: cadastro, gestão de ofertas,
 * gestão de pedidos recebidos e conversão de conta. As verificações de
 * permissão (ex: só o dono de uma oferta/pedido pode geri-lo) vivem aqui.
 */
@Service
public class ProdutorService {

    private final OfertaRepository ofertaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProdutorRepository produtorRepository;
    private final CompradorRepository compradorRepository;
    private final TransacaoRepository transacaoRepository;
    private final LocalizacaoService localizacaoService;
    private final ArmazenamentoService armazenamentoService;
    private final EmailService emailService;
    private final PasswordEncoderService passwordEncoderService;
    private final ConfirmacaoEmailService confirmacaoEmailService;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    public ProdutorService(OfertaRepository ofertaRepository,
                           PedidoRepository pedidoRepository,
                           ProdutorRepository produtorRepository,
                           CompradorRepository compradorRepository,
                           TransacaoRepository transacaoRepository,
                           LocalizacaoService localizacaoService,
                           ArmazenamentoService armazenamentoService,
                           EmailService emailService,
                           PasswordEncoderService passwordEncoderService,
                           ConfirmacaoEmailService confirmacaoEmailService) {
        this.ofertaRepository = ofertaRepository;
        this.pedidoRepository = pedidoRepository;
        this.produtorRepository = produtorRepository;
        this.compradorRepository = compradorRepository;
        this.transacaoRepository = transacaoRepository;
        this.localizacaoService = localizacaoService;
        this.armazenamentoService = armazenamentoService;
        this.emailService = emailService;
        this.passwordEncoderService = passwordEncoderService;
        this.confirmacaoEmailService = confirmacaoEmailService;
    }

    // ─── DASHBOARD ───────────────────────────────────────────────────────

    public record DashboardProdutor(Produtor produtor, long totalOfertas, long totalPedidos,
                                    long pedidosPendentes, long pedidosAceites,
                                    List<Oferta> ultimasOfertas, List<Pedido> ultimosPedidos,
                                    boolean jaEComprador, Long compradorId) {}

    public DashboardProdutor dashboard(Long produtorId) {
        Produtor produtor = produtorId != null ? produtorRepository.findById(produtorId).orElse(null) : null;

        long totalOfertas = 0, totalPedidos = 0, pedidosPendentes = 0, pedidosAceites = 0;
        List<Oferta> ultimasOfertas = new ArrayList<>();
        List<Pedido> ultimosPedidos = new ArrayList<>();

        if (produtor != null) {
            List<Oferta> minhasOfertas = ofertaRepository.ofertasPorProdutor(produtor.getId());
            totalOfertas = minhasOfertas.size();
            ultimasOfertas = minhasOfertas.stream()
                .sorted((a, b) -> b.getDataPublicacao().compareTo(a.getDataPublicacao()))
                .limit(3).toList();

            List<Pedido> todosPedidos = pedidoRepository.findByProdutorId(produtor.getId());
            totalPedidos = todosPedidos.size();
            pedidosPendentes = todosPedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.PENDENTE).count();
            pedidosAceites = todosPedidos.stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.ACEITE).count();
            ultimosPedidos = todosPedidos.stream()
                .filter(p -> p.getDataPedido() != null && p.getDataPedido().isAfter(LocalDateTime.now().minusDays(7)))
                .toList();
        }

        Long compradorIdAssociado = produtor != null
            ? compradorRepository.findByEmail(produtor.getEmail()).map(c -> c.getId()).orElse(null)
            : null;

        return new DashboardProdutor(produtor, totalOfertas, totalPedidos, pedidosPendentes, pedidosAceites,
            ultimasOfertas, ultimosPedidos, compradorIdAssociado != null, compradorIdAssociado);
    }

    // ─── CADASTRO ────────────────────────────────────────────────────────

    public Produtor cadastrar(Produtor produtor, MultipartFile fotoPerfilFile) {
        if (produtor.getEmail() != null && !produtor.getEmail().isBlank()
                && produtorRepository.existsByEmail(produtor.getEmail())) {
            throw new RegraNegocioException("Este e-mail já está registado. Faça login.");
        }
        if (produtor.getNuit() != null && !produtor.getNuit().isBlank()
                && produtorRepository.existsByNuit(produtor.getNuit())) {
            throw new RegraNegocioException("Este NUIT já está registado por outro produtor.");
        }

        produtor.setSenha(passwordEncoderService.encode(produtor.getSenha()));
        produtor.setEmailVerificado(false);

        Produtor salvo = produtorRepository.save(produtor);

        String foto = armazenamentoService.guardarFotoPerfil(fotoPerfilFile, "produtor", salvo.getId());
        if (foto != null) {
            salvo.setFotoPerfil(foto);
            produtorRepository.save(salvo);
        }
        confirmacaoEmailService.enviarConfirmacaoProdutor(salvo, baseUrl);
        return salvo;
    }

    // ─── PERFIL ──────────────────────────────────────────────────────────

    public Produtor obterPorId(Long id) {
        return produtorRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
    }

    public Produtor atualizarPerfil(Produtor produtorForm, MultipartFile fotoPerfilFile) {
        if (produtorForm.getEmail() != null && !produtorForm.getEmail().isBlank()) {
            produtorRepository.findByEmail(produtorForm.getEmail()).ifPresent(existente -> {
                if (!existente.getId().equals(produtorForm.getId()))
                    throw new RegraNegocioException("E-mail já registado por outro produtor.");
            });
        }

        Produtor existing = obterPorId(produtorForm.getId());

        if (produtorForm.getSenha() == null || produtorForm.getSenha().isBlank()) {
            produtorForm.setSenha(existing.getSenha());
        } else {
            produtorForm.setSenha(passwordEncoderService.encode(produtorForm.getSenha()));
        }

        String novaFoto = armazenamentoService.guardarFotoPerfil(fotoPerfilFile, "produtor", produtorForm.getId());
        produtorForm.setFotoPerfil(novaFoto != null ? novaFoto : existing.getFotoPerfil());

        return produtorRepository.save(produtorForm);
    }

    // ─── OFERTAS ─────────────────────────────────────────────────────────

    public Oferta publicarOferta(Oferta oferta, String provinciaNome, String distritoNome, String bairroNome,
                                 Long produtorId, MultipartFile imagemFile) {
        Bairro bairro = localizacaoService.resolverOuCriarBairro(provinciaNome, distritoNome, bairroNome);
        if (bairro != null) oferta.setBairro(bairro);

        String imagem = armazenamentoService.guardarImagemOferta(imagemFile);
        if (imagem != null) oferta.setImagemPath(imagem);

        Produtor produtor = produtorId != null ? produtorRepository.findById(produtorId).orElse(null) : null;
        if (produtor == null) throw new RegraNegocioException("Produtor não encontrado. Faça login primeiro.");

        oferta.setProdutor(produtor);
        return ofertaRepository.save(oferta);
    }

    public Oferta obterOferta(Long id) {
        return ofertaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Oferta não encontrada."));
    }

    /** Garante que a oferta pertence ao produtor indicado (regra de permissão). */
    private void garantirDonoDaOferta(Oferta oferta, Long produtorId) {
        if (produtorId != null && oferta.getProdutor() != null
                && !oferta.getProdutor().getId().equals(produtorId)) {
            throw new NaoAutorizadoException("Não tem permissão para gerir esta oferta.");
        }
    }

    public Oferta editarOferta(Long id, Oferta ofertaEditada, Long produtorId, MultipartFile imagemFile) {
        Oferta oferta = obterOferta(id);
        garantirDonoDaOferta(oferta, produtorId);

        oferta.setNomeProduto(ofertaEditada.getNomeProduto());
        oferta.setCategoria(ofertaEditada.getCategoria());
        oferta.setPreco(ofertaEditada.getPreco());
        oferta.setQuantidadeTotal(ofertaEditada.getQuantidadeTotal());
        oferta.setQuantidadeRestante(ofertaEditada.getQuantidadeRestante());
        oferta.setUnidade(ofertaEditada.getUnidade());
        oferta.setDescricao(ofertaEditada.getDescricao());
        oferta.setEstado(ofertaEditada.getEstado());
        if (ofertaEditada.getDataValidade() != null) oferta.setDataValidade(ofertaEditada.getDataValidade());

        String imagem = armazenamentoService.guardarImagemOferta(imagemFile);
        if (imagem != null) oferta.setImagemPath(imagem);

        return ofertaRepository.save(oferta);
    }

    public Long apagarOferta(Long id, Long produtorId) {
        Oferta oferta = obterOferta(id);
        garantirDonoDaOferta(oferta, produtorId);
        Long redirectId = produtorId != null ? produtorId : oferta.getProdutor().getId();
        ofertaRepository.deleteById(id);
        return redirectId;
    }

    public record MinhasOfertas(Produtor produtor, List<Oferta> ofertas) {}

    public MinhasOfertas minhasOfertas(Long produtorId) {
        if (produtorId == null) return new MinhasOfertas(null, ofertaRepository.findAll());
        Produtor produtor = produtorRepository.findById(produtorId).orElse(null);
        return new MinhasOfertas(produtor, ofertaRepository.ofertasPorProdutor(produtorId));
    }

    // ─── PEDIDOS RECEBIDOS ───────────────────────────────────────────────

    public record PedidosProdutor(Produtor produtor, List<Pedido> pedidos, Map<Long, Transacao> transacaoPorPedido) {}

    private PedidosProdutor pedidosDoProdutor(Long produtorId) {
        Produtor produtor = null;
        List<Pedido> pedidos;
        if (produtorId != null) {
            produtor = produtorRepository.findById(produtorId).orElse(null);
            pedidos = pedidoRepository.findByProdutorId(produtorId);
        } else {
            pedidos = pedidoRepository.findAll();
        }
        Map<Long, Transacao> transacaoPorPedido = new HashMap<>();
        for (Pedido p : pedidos) {
            transacaoRepository.findByPedidoId(p.getId()).ifPresent(t -> transacaoPorPedido.put(p.getId(), t));
        }
        return new PedidosProdutor(produtor, pedidos, transacaoPorPedido);
    }

    public PedidosProdutor pedidosRecebidos(Long produtorId) {
        return pedidosDoProdutor(produtorId);
    }

    public PedidosProdutor historicoVendas(Long produtorId) {
        return pedidosDoProdutor(produtorId);
    }

    /** Aceita um pedido: exige o número de pagamento e verifica que o pedido pertence a este produtor. */
    public void aceitarPedido(Long pedidoId, String numeroPagamento, Long produtorId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        garantirPedidoDoProdutor(pedido, produtorId);

        if (numeroPagamento == null || numeroPagamento.isBlank()) {
            throw new RegraNegocioException("Por favor, indique o número para receber o pagamento antes de aceitar.");
        }

        pedido.setNumeroPagamentoProdutor(numeroPagamento.trim());
        pedido.setEstado(Pedido.EstadoPedido.AGUARDANDO_PAGAMENTO);
        pedido.setDataResposta(LocalDateTime.now());
        pedidoRepository.save(pedido);

        if (pedido.getComprador() != null && pedido.getOferta() != null) {
            String assunto = "Pedido aceite — " + pedido.getOferta().getNomeProduto();
            String corpo = "Olá " + pedido.getComprador().getNomeCompleto() + ",\n\n"
                + "O seu pedido de \"" + pedido.getOferta().getNomeProduto() + "\" foi aceite pelo produtor.\n\n"
                + "Para concluir a compra, efectue o pagamento para o seguinte número:\n"
                + numeroPagamento.trim() + "\n\n"
                + "Depois de pagar, aceda à sua conta e confirme o pagamento na secção 'Os Meus Pedidos'.";
            emailService.enviar(pedido.getComprador().getEmail(), assunto, corpo);
        }
    }

    /** Rejeita um pedido: cancela a transação associada e restaura o stock da oferta. */
    public void rejeitarPedido(Long pedidoId, String motivoRejeicao, Long produtorId) {
        if (motivoRejeicao == null || motivoRejeicao.isBlank()) {
            throw new RegraNegocioException("Por favor, indique o motivo da rejeição antes de continuar.");
        }
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        garantirPedidoDoProdutor(pedido, produtorId);

        // O stock só é descontado quando o pedido é pago (ACEITE); só aí há stock a repor.
        boolean stockFoiDescontado = pedido.getEstado() == Pedido.EstadoPedido.ACEITE;

        pedido.setEstado(Pedido.EstadoPedido.REJEITADO);
        pedido.setDataResposta(LocalDateTime.now());
        pedido.setMotivoRejeicao(motivoRejeicao.trim());
        pedidoRepository.save(pedido);

        if (pedido.getComprador() != null && pedido.getOferta() != null) {
            String assunto = "Pedido rejeitado — " + pedido.getOferta().getNomeProduto();
            String corpo = "Olá " + pedido.getComprador().getNomeCompleto() + ",\n\n"
                + "Infelizmente o seu pedido de \"" + pedido.getOferta().getNomeProduto() + "\" foi rejeitado pelo produtor.\n\n"
                + "Motivo: " + motivoRejeicao.trim() + "\n";
            emailService.enviar(pedido.getComprador().getEmail(), assunto, corpo);
        }

        transacaoRepository.findByPedidoId(pedidoId).ifPresent(t -> {
            t.setEstado(Transacao.EstadoTransacao.CANCELADA);
            transacaoRepository.save(t);
        });

        Oferta oferta = pedido.getOferta();
        if (stockFoiDescontado && oferta != null && pedido.getQuantidade() != null
                && pedido.getQuantidade().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal restanteActual = oferta.getQuantidadeRestante() != null
                ? oferta.getQuantidadeRestante() : BigDecimal.ZERO;
            BigDecimal restaurado = restanteActual.add(pedido.getQuantidade());
            if (oferta.getQuantidadeTotal() != null && restaurado.compareTo(oferta.getQuantidadeTotal()) > 0) {
                restaurado = oferta.getQuantidadeTotal();
            }
            oferta.setQuantidadeRestante(restaurado);
            if (oferta.getEstado() == Oferta.EstadoOferta.INATIVO) {
                oferta.setEstado(Oferta.EstadoOferta.ATIVO);
            }
            ofertaRepository.save(oferta);
        }
    }

    /** Garante que o pedido pertence a uma oferta deste produtor (regra de permissão). */
    private void garantirPedidoDoProdutor(Pedido pedido, Long produtorId) {
        if (produtorId != null && pedido.getOferta() != null && pedido.getOferta().getProdutor() != null
                && !pedido.getOferta().getProdutor().getId().equals(produtorId)) {
            throw new NaoAutorizadoException("Não tem permissão para gerir este pedido.");
        }
    }
}
