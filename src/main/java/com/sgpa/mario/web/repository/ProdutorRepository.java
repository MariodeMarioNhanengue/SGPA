package com.example.demo.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.web.domain.Produtor;

@Repository
public interface ProdutorRepository extends JpaRepository<Produtor, Long> {

    // 🔐 Login / autenticação
    Optional<Produtor> findByEmail(String email);

    // 🔍 Buscar por nome
    Optional<Produtor> findFirstByNome(String nome);

    // ✅ NOVO: verificar duplicação de email
    boolean existsByEmail(String email);

    // ✅ NOVO: verificar duplicação de NUIT
    boolean existsByNuit(String nuit);

    Optional<Produtor> findByNuit(String nuit);
}