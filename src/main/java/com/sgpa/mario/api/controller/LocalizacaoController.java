package com.sgpa.mario.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgpa.mario.web.domain.Bairro;
import com.sgpa.mario.web.domain.Distrito;
import com.sgpa.mario.web.domain.Provincia;
import com.sgpa.mario.api.service.LocalizacaoService;

@RestController
@RequestMapping("/api/v1/localizacao")
public class LocalizacaoController {

    private final LocalizacaoService localizacaoService;

    public LocalizacaoController(LocalizacaoService localizacaoService) {
        this.localizacaoService = localizacaoService;
    }

    // =========================
    // PROVÍNCIA
    // =========================

    @GetMapping("/provincias")
    public List<Provincia> listarProvincias() {
        return localizacaoService.listarProvincias();
    }

    @PostMapping("/provincias")
    public Provincia criarProvincia(@RequestBody Provincia provincia) {
        return localizacaoService.criarProvincia(provincia);
    }

    // =========================
    // DISTRITO
    // =========================

    @GetMapping("/distritos/{provinciaId}")
    public List<Distrito> listarDistritos(@PathVariable Long provinciaId) {
        return localizacaoService.listarDistritos(provinciaId);
    }

    @PostMapping("/distritos")
    public Distrito criarDistrito(@RequestBody Distrito distrito) {
        return localizacaoService.criarDistrito(distrito);
    }

    // =========================
    // BAIRRO
    // =========================

    @GetMapping("/bairros/{distritoId}")
    public List<Bairro> listarBairros(@PathVariable Long distritoId) {
        return localizacaoService.listarBairros(distritoId);
    }

    @PostMapping("/bairros")
    public Bairro criarBairro(@RequestBody Bairro bairro) {
        return localizacaoService.criarBairro(bairro);
    }
}
