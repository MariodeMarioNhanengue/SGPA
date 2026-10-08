package com.sgpa.mario.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sgpa.mario.api.dto.CatalogoDtos.BairroRequest;
import com.sgpa.mario.api.dto.CatalogoDtos.BairroResponse;
import com.sgpa.mario.api.dto.CatalogoDtos.CategoriaResponse;
import com.sgpa.mario.api.dto.CatalogoDtos.DistritoRequest;
import com.sgpa.mario.api.dto.CatalogoDtos.DistritoResponse;
import com.sgpa.mario.api.dto.CatalogoDtos.ProdutoResponse;
import com.sgpa.mario.api.dto.CatalogoDtos.ProvinciaRequest;
import com.sgpa.mario.api.dto.CatalogoDtos.ProvinciaResponse;
import com.sgpa.mario.api.service.ApiCatalogoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catálogo e localização")
public class CatalogoApiController {

    private final ApiCatalogoService catalogoService;

    public CatalogoApiController(ApiCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Lista as categorias (público)")
    public List<CategoriaResponse> categorias() {
        return catalogoService.categorias();
    }

    @GetMapping("/produtos")
    @Operation(summary = "Lista os produtos do catálogo, com filtros opcionais (público)")
    public List<ProdutoResponse> produtos(@RequestParam(required = false) String nome,
                                          @RequestParam(required = false) String categoria) {
        return catalogoService.produtos(nome, categoria);
    }

    @GetMapping("/provincias")
    @Operation(summary = "Lista as províncias (público)")
    public List<ProvinciaResponse> provincias() {
        return catalogoService.provincias();
    }

    @GetMapping("/provincias/{provinciaId}/distritos")
    @Operation(summary = "Lista os distritos de uma província (público)")
    public List<DistritoResponse> distritos(@PathVariable Long provinciaId) {
        return catalogoService.distritos(provinciaId);
    }

    @GetMapping("/distritos/{distritoId}/bairros")
    @Operation(summary = "Lista os bairros de um distrito (público)")
    public List<BairroResponse> bairros(@PathVariable Long distritoId) {
        return catalogoService.bairros(distritoId);
    }

    @PostMapping("/provincias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria uma província (Admin)")
    public ProvinciaResponse criarProvincia(@Valid @RequestBody ProvinciaRequest req) {
        return catalogoService.criarProvincia(req);
    }

    @PostMapping("/distritos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria um distrito (Admin)")
    public DistritoResponse criarDistrito(@Valid @RequestBody DistritoRequest req) {
        return catalogoService.criarDistrito(req);
    }

    @PostMapping("/bairros")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria um bairro (Admin)")
    public BairroResponse criarBairro(@Valid @RequestBody BairroRequest req) {
        return catalogoService.criarBairro(req);
    }
}
