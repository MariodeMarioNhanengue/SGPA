package com.example.demo.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.web.domain.Oferta;
import com.example.demo.api.repository.CompradorRepository;
import com.example.demo.api.repository.OfertaRepository;
import com.example.demo.api.repository.ProdutorRepository;

@Controller
public class HomeControler {

    @Autowired
    private ProdutorRepository produtorRepository;

    @Autowired
    private CompradorRepository compradorRepository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("totalProdutores",  produtorRepository.count());
        model.addAttribute("totalCompradores", compradorRepository.count());
        model.addAttribute("totalOfertas",     ofertaRepository.countByEstado(Oferta.EstadoOferta.ATIVO));
        return "index";
    }

    @GetMapping("/sobre")
    public String sobre() {
        return "sobre";
    }

    @GetMapping("/como-funciona")
    public String comoFunciona() {
        return "como-funciona";
    }
}
