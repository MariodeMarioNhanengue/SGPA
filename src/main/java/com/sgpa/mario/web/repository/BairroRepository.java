package com.example.demo.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.web.domain.Bairro;
import com.example.demo.web.domain.Distrito;

import java.util.List;
import java.util.Optional;

@Repository
public interface BairroRepository extends JpaRepository<Bairro, Long> {

    Optional<Bairro> findByNome(String nome);

    Optional<Bairro> findByNomeAndDistrito(String nome, Distrito distrito);

    List<Bairro> findByNomeContainingIgnoreCase(String nome);

    List<Bairro> findAllByOrderByNomeAsc();

    List<Bairro> findByDistritoId(Long distritoId);
}