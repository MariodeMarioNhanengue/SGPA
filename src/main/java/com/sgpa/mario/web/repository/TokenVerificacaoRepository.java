package com.example.demo.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.web.domain.TokenVerificacao;

@Repository
public interface TokenVerificacaoRepository extends JpaRepository<TokenVerificacao, Long> {

    Optional<TokenVerificacao> findByTokenAndFinalidade(String token, TokenVerificacao.Finalidade finalidade);
}
