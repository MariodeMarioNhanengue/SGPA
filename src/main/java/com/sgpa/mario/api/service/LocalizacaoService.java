package com.example.demo.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Distrito;
import com.example.demo.web.domain.Provincia;
import com.example.demo.api.repository.BairroRepository;
import com.example.demo.api.repository.DistritoRepository;
import com.example.demo.api.repository.ProvinciaRepository;

@Service
public class LocalizacaoService {

    private final ProvinciaRepository provinciaRepository;
    private final DistritoRepository distritoRepository;
    private final BairroRepository bairroRepository;

    public LocalizacaoService(ProvinciaRepository provinciaRepository,
                              DistritoRepository distritoRepository,
                              BairroRepository bairroRepository) {
        this.provinciaRepository = provinciaRepository;
        this.distritoRepository = distritoRepository;
        this.bairroRepository = bairroRepository;
    }

    public List<Provincia> listarProvincias() {
        return provinciaRepository.findAll();
    }

    public Provincia criarProvincia(Provincia provincia) {
        return provinciaRepository.save(provincia);
    }

    public List<Distrito> listarDistritos(Long provinciaId) {
        return distritoRepository.findByProvinciaId(provinciaId);
    }

    public Distrito criarDistrito(Distrito distrito) {
        return distritoRepository.save(distrito);
    }

    public List<Bairro> listarBairros(Long distritoId) {
        return bairroRepository.findByDistritoId(distritoId);
    }

    public Bairro criarBairro(Bairro bairro) {
        return bairroRepository.save(bairro);
    }

    /**
     * Resolve (ou cria, se ainda não existir) a hierarquia Província → Distrito → Bairro
     * a partir dos nomes indicados, devolvendo o Bairro final para associação a uma entidade.
     * Devolve {@code null} caso os nomes não permitam chegar a um bairro.
     */
    public Bairro resolverOuCriarBairro(String provinciaNome, String distritoNome, String bairroNome) {
        if (provinciaNome == null || provinciaNome.isBlank()) return null;

        Provincia provincia = provinciaRepository.findByNome(provinciaNome)
            .orElseGet(() -> {
                Provincia p = new Provincia();
                p.setNome(provinciaNome);
                return provinciaRepository.save(p);
            });

        if (distritoNome == null || distritoNome.isBlank()) return null;

        Distrito distrito = distritoRepository.findByNomeAndProvincia(distritoNome, provincia)
            .orElseGet(() -> {
                Distrito d = new Distrito();
                d.setNome(distritoNome);
                d.setProvincia(provincia);
                return distritoRepository.save(d);
            });

        if (bairroNome == null || bairroNome.isBlank()) return null;

        return bairroRepository.findByNomeAndDistrito(bairroNome, distrito)
            .orElseGet(() -> {
                Bairro b = new Bairro();
                b.setNome(bairroNome);
                b.setDistrito(distrito);
                return bairroRepository.save(b);
            });
    }
}
