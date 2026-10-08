package com.sgpa.mario.api.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgpa.mario.web.domain.Distrito;
import com.sgpa.mario.web.domain.Provincia;

public interface DistritoRepository extends JpaRepository<Distrito, Long> {

	    List<Distrito> findByProvinciaId(Long provinciaId);
	    Optional<Distrito> findByNomeAndProvincia(String nome, Provincia provincia);

	}
