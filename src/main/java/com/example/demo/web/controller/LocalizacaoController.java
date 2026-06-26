package com.example.demo.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Distrito;
import com.example.demo.web.domain.Provincia;
import com.example.demo.web.repository.BairroRepository;
import com.example.demo.web.repository.DistritoRepository;
import com.example.demo.web.repository.ProvinciaRepository;

@RestController
@RequestMapping("/api")
public class LocalizacaoController {

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private DistritoRepository distritoRepository;

    @Autowired
    private BairroRepository bairroRepository;

    // =========================
    // PROVÍNCIA
    // =========================

    @GetMapping("/provincias")
    public List<Provincia> listarProvincias() {
        return provinciaRepository.findAll();
    }

    @SuppressWarnings("null")
	@PostMapping("/provincias")
    public Provincia criarProvincia(@RequestBody Provincia provincia) {
        return provinciaRepository.save(provincia);
    }

    // =========================
    // DISTRITO
    // =========================

    @GetMapping("/distritos/{provinciaId}")
    public List<Distrito> listarDistritos(@PathVariable Long provinciaId) {
        return distritoRepository.findByProvinciaId(provinciaId);
    }

    @SuppressWarnings("null")
	@PostMapping("/distritos")
    public Distrito criarDistrito(@RequestBody Distrito distrito) {
        return distritoRepository.save(distrito);
    }

    // =========================
    // BAIRRO
    // =========================

    @GetMapping("/bairros/{distritoId}")
    public List<Bairro> listarBairros(@PathVariable Long distritoId) {
        return bairroRepository.findByDistritoId(distritoId);
    }

    @SuppressWarnings("null")
	@PostMapping("/bairros")
    public Bairro criarBairro(@RequestBody Bairro bairro) {
        return bairroRepository.save(bairro);
    }
}