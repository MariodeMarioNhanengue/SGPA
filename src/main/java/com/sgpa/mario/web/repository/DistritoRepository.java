package com.example.demo.web.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.web.domain.Distrito;
import com.example.demo.web.domain.Provincia;

public interface DistritoRepository extends JpaRepository<Distrito, Long> {

	    List<Distrito> findByProvinciaId(Long provinciaId);
	    Optional<Distrito> findByNomeAndProvincia(String nome, Provincia provincia);

	}
