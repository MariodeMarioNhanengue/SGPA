package com.sgpa.mario.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgpa.mario.web.domain.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // 🔎 Buscar por nome
    List<Produto> findByNomeContainingIgnoreCase(String nome);

    // 🔎 Buscar por categoria (via relação)
    List<Produto> findByCategoriaNomeIgnoreCase(String nomeCategoria);

    // 🔥 Nome + Categoria (RELACIONAL CORRETO)
    List<Produto> findByNomeContainingIgnoreCaseAndCategoriaNomeIgnoreCase(
            String nome,
            String categoria
    );
}