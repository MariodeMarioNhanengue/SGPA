package com.example.demo.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.demo.web.domain.Oferta;

/** Pesquisa dinâmica e paginada de ofertas (filtros opcionais combináveis). */
@Repository
public interface OfertaSearchRepository extends JpaRepository<Oferta, Long>, JpaSpecificationExecutor<Oferta> {
}
