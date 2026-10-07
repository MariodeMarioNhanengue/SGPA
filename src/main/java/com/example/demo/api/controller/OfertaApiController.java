package com.example.demo.api.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.api.config.UsuarioAtual;
import com.example.demo.api.dto.OfertaDtos.EstadoOfertaRequest;
import com.example.demo.api.dto.OfertaDtos.OfertaRequest;
import com.example.demo.api.dto.OfertaDtos.OfertaResponse;
import com.example.demo.api.dto.PageResponse;
import com.example.demo.api.dto.UsuarioDtos.ResumoProdutorResponse;
import com.example.demo.api.service.ApiOfertaService;
import com.example.demo.api.service.OfertaFiltro;
import com.example.demo.web.domain.Oferta.EstadoOferta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ofertas")
public class OfertaApiController {

    private final ApiOfertaService ofertaService;

    public OfertaApiController(ApiOfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    // ─── Catálogo público ────────────────────────────────────────────────

    @GetMapping("/ofertas")
    @Operation(summary = "Pesquisa ofertas (público, paginado, filtros combináveis)")
    public PageResponse<OfertaResponse> pesquisar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Long provinciaId,
            @RequestParam(required = false) Long distritoId,
            @RequestParam(required = false) Long bairroId,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(required = false) Long produtorId,
            @RequestParam(required = false) EstadoOferta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @Parameter(description = "recentes (por omissão) | preco_asc | preco_desc | nome")
            @RequestParam(defaultValue = "recentes") String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        OfertaFiltro filtro = new OfertaFiltro(nome, categoria, provinciaId, distritoId, bairroId,
            precoMin, precoMax, produtorId, estado, dataInicio, dataFim);
        Pageable pageable = PageRequest.of(Paginacao.pagina(pagina), Paginacao.tamanho(tamanho), ordenacao(ordem));
        return PageResponse.de(ofertaService.pesquisar(filtro, pageable));
    }

    @GetMapping("/ofertas/{id}")
    @Operation(summary = "Detalhe de uma oferta (público)")
    public OfertaResponse obter(@PathVariable Long id) {
        return ofertaService.obter(id);
    }

    // ─── Produtor ────────────────────────────────────────────────────────

    @PostMapping("/ofertas")
    @PreAuthorize("hasRole('PRODUTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publica uma oferta (Produtor)")
    public ResponseEntity<OfertaResponse> criar(@Valid @RequestBody OfertaRequest req) {
        OfertaResponse criada = ofertaService.criar(UsuarioAtual.obter().id(), req);
        return ResponseEntity.created(URI.create("/api/v1/ofertas/" + criada.id())).body(criada);
    }

    @PutMapping("/ofertas/{id}")
    @PreAuthorize("hasRole('PRODUTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edita uma oferta própria (Produtor)")
    public OfertaResponse atualizar(@PathVariable Long id, @Valid @RequestBody OfertaRequest req) {
        return ofertaService.atualizar(UsuarioAtual.obter().id(), id, req);
    }

    @PatchMapping("/ofertas/{id}/estado")
    @PreAuthorize("hasRole('PRODUTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activa/desactiva uma oferta própria (ATIVO, INATIVO, EXPIRADO)")
    public OfertaResponse alterarEstado(@PathVariable Long id, @Valid @RequestBody EstadoOfertaRequest req) {
        return ofertaService.alterarEstado(UsuarioAtual.obter().id(), id, req.estado());
    }

    @PutMapping(value = "/ofertas/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PRODUTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Envia a imagem da oferta (multipart, campo 'imagem')")
    public OfertaResponse imagem(@PathVariable Long id, @RequestPart("imagem") MultipartFile imagem) {
        return ofertaService.definirImagem(UsuarioAtual.obter().id(), id, imagem);
    }

    @DeleteMapping("/ofertas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PRODUTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Apaga uma oferta própria sem pedidos associados")
    public void apagar(@PathVariable Long id) {
        ofertaService.apagar(UsuarioAtual.obter().id(), id);
    }

    @GetMapping("/produtor/ofertas")
    @PreAuthorize("hasRole('PRODUTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "As minhas ofertas (todos os estados)")
    public PageResponse<OfertaResponse> minhasOfertas(@RequestParam(defaultValue = "0") int pagina,
                                                      @RequestParam(defaultValue = "20") int tamanho) {
        return PageResponse.de(ofertaService.minhasOfertas(UsuarioAtual.obter().id()),
            Paginacao.pagina(pagina), Paginacao.tamanho(tamanho));
    }

    @GetMapping("/produtor/resumo")
    @PreAuthorize("hasRole('PRODUTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Resumo do painel do produtor")
    public ResumoProdutorResponse resumo() {
        return ofertaService.resumo(UsuarioAtual.obter().id());
    }

    private Sort ordenacao(String ordem) {
        return switch (ordem == null ? "" : ordem) {
            case "preco_asc" -> Sort.by(Sort.Direction.ASC, "preco");
            case "preco_desc" -> Sort.by(Sort.Direction.DESC, "preco");
            case "nome" -> Sort.by(Sort.Direction.ASC, "nomeProduto");
            default -> Sort.by(Sort.Direction.DESC, "dataPublicacao");
        };
    }
}
