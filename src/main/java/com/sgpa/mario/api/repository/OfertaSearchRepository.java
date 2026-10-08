package com.sgpa.mario.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sgpa.mario.web.domain.Oferta;

/** Pesquisa dinâmica e paginada de ofertas (filtros opcionais combináveis). */
@Repository
public interface OfertaSearchRepository extends JpaRepository<Oferta, Long>, JpaSpecificationExecutor<Oferta> {
}
