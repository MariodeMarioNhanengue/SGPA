package com.example.demo.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.web.domain.Comprador;

@Repository
public interface CompradorRepository extends JpaRepository<Comprador, Long> {

    Optional<Comprador> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNuit(String nuit);

    Optional<Comprador> findByNuit(String nuit);
}
