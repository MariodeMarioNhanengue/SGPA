package com.example.demo.api.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.api.dto.CatalogoDtos.BairroRequest;
import com.example.demo.api.dto.CatalogoDtos.BairroResponse;
import com.example.demo.api.dto.CatalogoDtos.CategoriaResponse;
import com.example.demo.api.dto.CatalogoDtos.DistritoRequest;
import com.example.demo.api.dto.CatalogoDtos.DistritoResponse;
import com.example.demo.api.dto.CatalogoDtos.ProdutoResponse;
import com.example.demo.api.dto.CatalogoDtos.ProvinciaRequest;
import com.example.demo.api.dto.CatalogoDtos.ProvinciaResponse;
import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Distrito;
import com.example.demo.web.domain.Produto;
import com.example.demo.web.domain.Provincia;
import com.example.demo.api.repository.BairroRepository;
import com.example.demo.api.repository.CategoriaRepository;
import com.example.demo.api.repository.DistritoRepository;
import com.example.demo.api.repository.ProdutoRepository;
import com.example.demo.api.repository.ProvinciaRepository;
import com.example.demo.api.service.LocalizacaoService;
import com.example.demo.api.service.exception.RecursoNaoEncontradoException;
import com.example.demo.api.service.exception.RegraNegocioException;

/** Categorias, produtos e hierarquia Província > Distrito > Bairro. */
@Service
public class ApiCatalogoService {

    private final LocalizacaoService localizacaoService;
    private final ProvinciaRepository provinciaRepository;
    private final DistritoRepository distritoRepository;
    private final BairroRepository bairroRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public ApiCatalogoService(LocalizacaoService localizacaoService, ProvinciaRepository provinciaRepository,
                              DistritoRepository distritoRepository, BairroRepository bairroRepository,
                              CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.localizacaoService = localizacaoService;
        this.provinciaRepository = provinciaRepository;
        this.distritoRepository = distritoRepository;
        this.bairroRepository = bairroRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    // ─── Localização (leitura) ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProvinciaResponse> provincias() {
        return localizacaoService.listarProvincias().stream()
            .sorted(Comparator.comparing(Provincia::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(ApiMapper::provincia).toList();
    }

    @Transactional(readOnly = true)
    public List<DistritoResponse> distritos(Long provinciaId) {
        if (!provinciaRepository.existsById(provinciaId)) {
            throw new RecursoNaoEncontradoException("Província não encontrada.");
        }
        return localizacaoService.listarDistritos(provinciaId).stream()
            .sorted(Comparator.comparing(Distrito::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(ApiMapper::distrito).toList();
    }

    @Transactional(readOnly = true)
    public List<BairroResponse> bairros(Long distritoId) {
        if (!distritoRepository.existsById(distritoId)) {
            throw new RecursoNaoEncontradoException("Distrito não encontrado.");
        }
        return localizacaoService.listarBairros(distritoId).stream()
            .sorted(Comparator.comparing(Bairro::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(ApiMapper::bairro).toList();
    }

    // ─── Localização (admin) ─────────────────────────────────────────────

    @Transactional
    public ProvinciaResponse criarProvincia(ProvinciaRequest r) {
        String nome = r.nome().trim();
        if (provinciaRepository.findByNome(nome).isPresent()) {
            throw new RegraNegocioException("Já existe uma província com este nome.");
        }
        Provincia p = new Provincia();
        p.setNome(nome);
        return ApiMapper.provincia(localizacaoService.criarProvincia(p));
    }

    @Transactional
    public DistritoResponse criarDistrito(DistritoRequest r) {
        Provincia provincia = provinciaRepository.findById(r.provinciaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Província não encontrada."));
        String nome = r.nome().trim();
        if (distritoRepository.findByNomeAndProvincia(nome, provincia).isPresent()) {
            throw new RegraNegocioException("Já existe um distrito com este nome nesta província.");
        }
        Distrito d = new Distrito();
        d.setNome(nome);
        d.setProvincia(provincia);
        return ApiMapper.distrito(localizacaoService.criarDistrito(d));
    }

    @Transactional
    public BairroResponse criarBairro(BairroRequest r) {
        Distrito distrito = distritoRepository.findById(r.distritoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Distrito não encontrado."));
        String nome = r.nome().trim();
        if (bairroRepository.findByNomeAndDistrito(nome, distrito).isPresent()) {
            throw new RegraNegocioException("Já existe um bairro com este nome neste distrito.");
        }
        Bairro b = new Bairro();
        b.setNome(nome);
        b.setDistrito(distrito);
        return ApiMapper.bairro(localizacaoService.criarBairro(b));
    }

    // ─── Categorias e produtos ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias() {
        return categoriaRepository.findAll().stream()
            .map(c -> new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao())).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> produtos(String nome, String categoria) {
        boolean temNome = nome != null && !nome.isBlank();
        boolean temCategoria = categoria != null && !categoria.isBlank();
        List<Produto> lista;
        if (temNome && temCategoria) {
            lista = produtoRepository.findByNomeContainingIgnoreCaseAndCategoriaNomeIgnoreCase(nome.trim(), categoria.trim());
        } else if (temNome) {
            lista = produtoRepository.findByNomeContainingIgnoreCase(nome.trim());
        } else if (temCategoria) {
            lista = produtoRepository.findByCategoriaNomeIgnoreCase(categoria.trim());
        } else {
            lista = produtoRepository.findAll();
        }
        return lista.stream()
            .map(p -> new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getUnidade(),
                p.getCategoria() == null ? null : p.getCategoria().getNome()))
            .toList();
    }
}
