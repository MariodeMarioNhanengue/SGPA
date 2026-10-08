package com.sgpa.mario.api.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Protecção simples contra ataques de força bruta no login: depois de várias
 * tentativas falhadas seguidas para o mesmo email, bloqueia novas tentativas
 * durante algum tempo — mesmo que a senha correcta seja introduzida entretanto.
 *
 * Implementação em memória (não persiste em BD nem sobrevive a um reinício da
 * aplicação), o que é suficiente para uma única instância do servidor.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);

    private record Registo(int falhas, LocalDateTime bloqueadoAte) {}

    private final Map<String, Registo> tentativas = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String email) {
        Registo r = tentativas.get(normalizar(email));
        return r != null && r.bloqueadoAte() != null && LocalDateTime.now().isBefore(r.bloqueadoAte());
    }

    /** Minutos restantes de bloqueio (arredondado para cima, mínimo 1 enquanto estiver bloqueado). */
    public long minutosRestantes(String email) {
        Registo r = tentativas.get(normalizar(email));
        if (r == null || r.bloqueadoAte() == null) return 0;
        long segundos = Duration.between(LocalDateTime.now(), r.bloqueadoAte()).getSeconds();
        return segundos <= 0 ? 0 : (segundos + 59) / 60;
    }

    public void registarFalha(String email) {
        String chave = normalizar(email);
        tentativas.compute(chave, (k, atual) -> {
            int falhas = (atual == null ? 0 : atual.falhas()) + 1;
            LocalDateTime bloqueadoAte = falhas >= MAX_TENTATIVAS ? LocalDateTime.now().plus(DURACAO_BLOQUEIO) : null;
            return new Registo(falhas, bloqueadoAte);
        });
    }

    public void registarSucesso(String email) {
        tentativas.remove(normalizar(email));
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
