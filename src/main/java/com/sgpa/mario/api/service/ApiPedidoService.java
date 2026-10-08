package com.sgpa.mario.api.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgpa.mario.api.config.UsuarioAtual;
import com.sgpa.mario.api.dto.PedidoDtos.PagamentoRequest;
import com.sgpa.mario.api.dto.PedidoDtos.PedidoCreateRequest;
import com.sgpa.mario.api.dto.PedidoDtos.PedidoResponse;
import com.sgpa.mario.api.exception.ApiException;
import com.sgpa.mario.web.domain.Oferta;
import com.sgpa.mario.web.domain.Pedido;
import com.sgpa.mario.web.domain.Pedido.EstadoPedido;
import com.sgpa.mario.web.domain.Transacao.MetodoPagamento;
import com.sgpa.mario.web.domain.Transacao.Operadora;
import com.sgpa.mario.api.repository.PedidoRepository;
import com.sgpa.mario.api.repository.TransacaoRepository;
import com.sgpa.mario.api.service.CompradorService;
import com.sgpa.mario.api.service.ProdutorService;
import com.sgpa.mario.api.service.exception.NaoAutorizadoException;
import com.sgpa.mario.api.service.exception.RecursoNaoEncontradoException;
import com.sgpa.mario.api.service.exception.RegraNegocioException;

/**
 * Ciclo de vida do pedido:
 * PENDENTE -> (produtor aceita) AGUARDANDO_PAGAMENTO -> (comprador paga) ACEITE;
 * rejeição pelo produtor (REJEITADO) e cancelamento pelo comprador (CANCELADO, só se PENDENTE).
 */
@Service
public class ApiPedidoService {

    private static final Set<Operadora> MOVEIS = EnumSet.of(Operadora.MPESA, Operadora.EMOLA, Operadora.MKESH, Operadora.OUTROS);
    private static final Set<Operadora> BANCOS = EnumSet.of(Operadora.BCI, Operadora.BIM, Operadora.STANDARD_BANK,
        Operadora.VISA, Operadora.OUTROS);

    private final PedidoRepository pedidoRepository;
    private final TransacaoRepository transacaoRepository;
    private final CompradorService compradorService;
    private final ProdutorService produtorService;

    public ApiPedidoService(PedidoRepository pedidoRepository, TransacaoRepository transacaoRepository,
                            CompradorService compradorService, ProdutorService produtorService) {
        this.pedidoRepository = pedidoRepository;
        this.transacaoRepository = transacaoRepository;
        this.compradorService = compradorService;
        this.produtorService = produtorService;
    }

    // ─── Mapeamento ──────────────────────────────────────────────────────

    public PedidoResponse mapear(Pedido p) {
        return ApiMapper.pedido(p, transacaoRepository.findByPedidoId(p.getId()).orElse(null));
    }

    public List<PedidoResponse> mapear(List<Pedido> pedidos) {
        return pedidos.stream()
            .sorted(Comparator.comparing(Pedido::getDataPedido, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::mapear)
            .toList();
    }

    // ─── Consulta ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(UsuarioAtual.Dados u, EstadoPedido estado) {
        List<Pedido> base = switch (u.tipo()) {
            case UsuarioAtual.COMPRADOR -> pedidoRepository.findByCompradorId(u.id());
            case UsuarioAtual.PRODUTOR -> pedidoRepository.findByProdutorId(u.id());
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO",
                "Use /api/v1/admin/vendas para listar todos os pedidos.");
        };
        List<Pedido> filtrados = estado == null ? base
            : base.stream().filter(p -> p.getEstado() == estado).toList();
        return mapear(filtrados);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obter(UsuarioAtual.Dados u, Long id) {
        Pedido p = carregar(id);
        boolean donoComprador = UsuarioAtual.COMPRADOR.equals(u.tipo())
            && p.getComprador() != null && p.getComprador().getId().equals(u.id());
        boolean donoProdutor = UsuarioAtual.PRODUTOR.equals(u.tipo()) && p.getOferta() != null
            && p.getOferta().getProdutor() != null && p.getOferta().getProdutor().getId().equals(u.id());
        if (!(donoComprador || donoProdutor || UsuarioAtual.ADMIN.equals(u.tipo()))) {
            throw new NaoAutorizadoException("Não tem permissão para ver este pedido.");
        }
        return mapear(p);
    }

    // ─── Comprador ───────────────────────────────────────────────────────

    @Transactional
    public PedidoResponse criar(Long compradorId, PedidoCreateRequest r) {
        Pedido p = compradorService.comprar(r.ofertaId(), compradorId, r.quantidade(), UsuarioAtual.COMPRADOR, compradorId);
        return mapear(p);
    }

    @Transactional
    public PedidoResponse cancelar(Long compradorId, Long id) {
        compradorService.cancelarPedido(id, compradorId);
        return mapear(carregar(id));
    }

    @Transactional
    public PedidoResponse pagar(Long compradorId, Long id, PagamentoRequest r) {
        Pedido p = carregar(id);
        if (p.getComprador() == null || !p.getComprador().getId().equals(compradorId)) {
            throw new NaoAutorizadoException("Não tem permissão para pagar este pedido.");
        }
        if (p.getEstado() != EstadoPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("Este pedido não está a aguardar pagamento (estado actual: " + p.getEstado() + ").");
        }
        if (transacaoRepository.findByPedidoId(id).isPresent()) {
            throw new RegraNegocioException("Este pedido já tem um pagamento registado.");
        }
        if (r.metodoPagamento() == MetodoPagamento.CONTA_MOVEL && !MOVEIS.contains(r.operadora())) {
            throw new RegraNegocioException("Para CONTA_MOVEL a operadora deve ser MPESA, EMOLA ou MKESH.");
        }
        if (r.metodoPagamento() == MetodoPagamento.BANCO && !BANCOS.contains(r.operadora())) {
            throw new RegraNegocioException("Para BANCO a operadora deve ser BCI, BIM, STANDARD_BANK, VISA ou OUTROS.");
        }
        Oferta o = p.getOferta();
        BigDecimal quantidade = p.getQuantidade();
        if (o == null || quantidade == null || quantidade.signum() <= 0) {
            throw new RegraNegocioException("Pedido sem quantidade válida.");
        }
        BigDecimal restante = o.getQuantidadeRestante() != null ? o.getQuantidadeRestante() : BigDecimal.ZERO;
        if (restante.compareTo(quantidade) < 0) {
            throw new RegraNegocioException("Stock insuficiente: restam " + restante + " " + o.getUnidade() + ".");
        }
        compradorService.enviarPagamento(id, r.metodoPagamento().name(), r.operadora().name(), r.referencia());
        return mapear(carregar(id));
    }

    // ─── Produtor ────────────────────────────────────────────────────────

    @Transactional
    public PedidoResponse aceitar(Long produtorId, Long id, String numeroPagamento) {
        Pedido p = pedidoDoProdutor(id, produtorId);
        if (p.getEstado() != EstadoPedido.PENDENTE) {
            throw new RegraNegocioException("Só é possível aceitar pedidos pendentes (estado actual: " + p.getEstado() + ").");
        }
        produtorService.aceitarPedido(id, numeroPagamento, produtorId);
        return mapear(carregar(id));
    }

    @Transactional
    public PedidoResponse rejeitar(Long produtorId, Long id, String motivo) {
        Pedido p = pedidoDoProdutor(id, produtorId);
        EstadoPedido e = p.getEstado();
        if (e != EstadoPedido.PENDENTE && e != EstadoPedido.AGUARDANDO_PAGAMENTO && e != EstadoPedido.ACEITE) {
            throw new RegraNegocioException("Este pedido já não pode ser rejeitado (estado actual: " + e + ").");
        }
        produtorService.rejeitarPedido(id, motivo, produtorId);
        return mapear(carregar(id));
    }

    // ─── Auxiliares ──────────────────────────────────────────────────────

    private Pedido carregar(Long id) {
        return pedidoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    private Pedido pedidoDoProdutor(Long id, Long produtorId) {
        Pedido p = carregar(id);
        if (p.getOferta() == null || p.getOferta().getProdutor() == null
                || !p.getOferta().getProdutor().getId().equals(produtorId)) {
            throw new NaoAutorizadoException("Não tem permissão para gerir este pedido.");
        }
        return p;
    }
}
