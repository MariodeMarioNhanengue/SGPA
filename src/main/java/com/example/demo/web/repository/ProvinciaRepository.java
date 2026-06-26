package com.example.demo.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.example.demo.web.domain.Provincia;

@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    Optional<Provincia> findByNome(String nome);
}