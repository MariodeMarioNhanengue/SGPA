package com.example.demo.api.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuração do JWT. Defina no application.properties (ou variáveis de ambiente):
 *   app.jwt.secret=<mínimo 32 caracteres>      (ou variável JWT_SECRET via app.jwt.secret=${JWT_SECRET})
 *   app.jwt.expiration-minutes=60
 * Sem segredo configurado, gera-se um aleatório no arranque (os tokens deixam de valer ao reiniciar).
 */
@Component
public class JwtProperties {

    private static final Logger log = LoggerFactory.getLogger(JwtProperties.class);

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtProperties(@Value("${app.jwt.secret:}") String segredo,
                         @Value("${app.jwt.expiration-minutes:60}") long expiracaoMinutos) {
        byte[] bytes;
        if (segredo == null || segredo.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("app.jwt.secret não definido: foi gerado um segredo temporário. "
                   + "Defina app.jwt.secret (>= 32 caracteres) para os tokens sobreviverem a reinícios.");
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("app.jwt.secret deve ter pelo menos 32 caracteres.");
            }
        }
        this.chave = new SecretKeySpec(bytes, "HmacSHA256");
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public SecretKey getChave() { return chave; }
    public long getExpiracaoMinutos() { return expiracaoMinutos; }
}
