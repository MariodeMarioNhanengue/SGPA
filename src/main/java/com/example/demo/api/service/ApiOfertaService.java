package com.example.demo.api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.api.dto.OfertaDtos.OfertaRequest;
import com.example.demo.api.dto.OfertaDtos.OfertaResponse;
import com.example.demo.api.dto.UsuarioDtos.ResumoProdutorResponse;
import com.example.demo.api.exception.ApiException;
import com.example.demo.api.repository.OfertaSearchRepository;
import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Oferta;
import com.example.demo.web.domain.Oferta.EstadoOferta;
import com.example.demo.api.repository.OfertaRepository;
import com.example.demo.api.service.ArmazenamentoService;
import com.example.demo.api.service.LocalizacaoService;
import com.example.demo.api.service.ProdutorService;
import com.example.demo.api.service.exception.NaoAutorizadoException;
import com.example.demo.api.service.exception.RecursoNaoEncontradoException;
import com.example.demo.api.service.exception.RegraNegocioException;

/** Catálogo público de ofertas e gestão das ofertas pelo produtor dono. */
@Service
public class ApiOfertaService {

    private final OfertaRepository ofertaRepository;
    private final OfertaSearchRepository ofertaSearchRepository;
    private final ProdutorService produtorService;
    private final LocalizacaoService localizacaoService;
    private final ArmazenamentoService armazenamentoService;

    public ApiOfertaService(OfertaRepository ofertaRepository, OfertaSearchRepository ofertaSearchRepository,
                            ProdutorService produtorService, LocalizacaoService localizacaoService,
                            ArmazenamentoService armazenamentoService) {
        this.ofertaRepository = ofertaRepository;
        this.ofertaSearchRepository = ofertaSearchRepository;
        this.produtorService = produtorService;
        this.localizacaoService = localizacaoService;
        this.armazenamentoService = armazenamentoService;
    }

    // ─── Público ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OfertaResponse> pesquisar(OfertaFiltro filtro, Pageable pageable) {
        return ofertaSearchRepository.findAll(OfertaSpecs.filtro(filtro), pageable).map(ApiMapper::oferta);
    }

    @Transactional(readOnly = true)
    public OfertaResponse obter(Long id) {
        return ApiMapper.oferta(ofertaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Oferta não encontrada.")));
    }

    // ─── Produtor ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OfertaResponse> minhasOfertas(Long produtorId) {
        return ofertaRepository.ofertasPorProdutor(produtorId).stream()
            .sorted(Comparator.comparing(Oferta::getDataPublicacao, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(ApiMapper::oferta)
            .toList();
    }

    @Transactional(readOnly = true)
    public ResumoProdutorResponse resumo(Long produtorId) {
        var d = produtorService.dashboard(produtorId);
        return new ResumoProdutorResponse(d.totalOfertas(), d.totalPedidos(), d.pedidosPendentes(), d.pedidosAceites());
    }

    @Transactional
    public OfertaResponse criar(Long produtorId, OfertaRequest r) {
        validarStock(r.quantidadeTotal(), r.quantidadeRestante());
        validarValidade(r.dataValidade());
        Oferta o = new Oferta();
        preencher(o, r);
        o.setQuantidadeRestante(r.quantidadeRestante()); // null => igual ao total (prePersist)
        o.setEstado(r.estado() != null ? r.estado() : EstadoOferta.ATIVO);
        Oferta salva = produtorService.publicarOferta(o, r.provincia(), r.distrito(), r.bairro(), produtorId, null);
        return ApiMapper.oferta(salva);
    }

    @Transactional
    public OfertaResponse atualizar(Long produtorId, Long id, OfertaRequest r) {
        Oferta existente = ofertaDoProdutor(id, produtorId);
        BigDecimal restante = r.quantidadeRestante() != null ? r.quantidadeRestante() : existente.getQuantidadeRestante();
        validarStock(r.quantidadeTotal(), restante);
        if (r.dataValidade() != null && !r.dataValidade().equals(existente.getDataValidade())) {
            validarValidade(r.dataValidade());
        }
        Oferta editada = new Oferta();
        preencher(editada, r);
        editada.setQuantidadeRestante(restante);
        editada.setEstado(r.estado() != null ? r.estado() : existente.getEstado());
        Oferta salva = produtorService.editarOferta(id, editada, produtorId, null);
        if (r.provincia() != null && !r.provincia().isBlank()) {
            Bairro b = localizacaoService.resolverOuCriarBairro(r.provincia(), r.distrito(), r.bairro());
            if (b != null) {
                salva.setBairro(b);
                salva = ofertaRepository.save(salva);
            }
        }
        return ApiMapper.oferta(salva);
    }

    @Transactional
    public OfertaResponse alterarEstado(Long produtorId, Long id, EstadoOferta estado) {
        Oferta o = ofertaDoProdutor(id, produtorId);
        o.setEstado(estado);
        return ApiMapper.oferta(ofertaRepository.save(o));
    }

    @Transactional
    public OfertaResponse definirImagem(Long produtorId, Long id, MultipartFile imagem) {
        ImagemUtil.validar(imagem);
        Oferta o = ofertaDoProdutor(id, produtorId);
        String url = armazenamentoService.guardarImagemOferta(imagem);
        if (url == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_GUARDAR_FICHEIRO",
                "Não foi possível guardar a imagem.");
        }
        o.setImagemPath(url);
        return ApiMapper.oferta(ofertaRepository.save(o));
    }

    /** Sem @Transactional de propósito: o erro de integridade (pedidos associados) surge aqui e vira 409 claro. */
    public void apagar(Long produtorId, Long id) {
        ofertaDoProdutor(id, produtorId);
        try {
            produtorService.apagarOferta(id, produtorId);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "OFERTA_COM_PEDIDOS",
                "Esta oferta já tem pedidos associados e não pode ser apagada. Altere o estado para INATIVO.");
        }
    }

    // ─── Auxiliares ──────────────────────────────────────────────────────

    private Oferta ofertaDoProdutor(Long id, Long produtorId) {
        Oferta o = ofertaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Oferta não encontrada."));
        if (o.getProdutor() == null || !o.getProdutor().getId().equals(produtorId)) {
            throw new NaoAutorizadoException("Não tem permissão para gerir esta oferta.");
        }
        return o;
    }

    private void preencher(Oferta o, OfertaRequest r) {
        o.setNomeProduto(r.nomeProduto().trim());
        o.setCategoria(r.categoria().trim());
        o.setUnidade(r.unidade().trim());
        o.setQuantidadeTotal(r.quantidadeTotal());
        o.setPreco(r.preco());
        o.setDescricao(r.descricao());
        o.setDataValidade(r.dataValidade());
    }

    private void validarStock(BigDecimal total, BigDecimal restante) {
        if (restante != null && restante.compareTo(total) > 0) {
            throw new RegraNegocioException("A quantidade restante não pode exceder a quantidade total.");
        }
    }

    private void validarValidade(LocalDateTime validade) {
        if (validade != null && validade.isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("A data de validade deve ser futura.");
        }
    }
}
