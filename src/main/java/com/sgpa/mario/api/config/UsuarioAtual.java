package com.sgpa.mario.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.sgpa.mario.api.exception.ApiException;

/** Acesso ao utilizador autenticado (lido do JWT). */
public final class UsuarioAtual {

    public static final String PRODUTOR = "PRODUTOR";
    public static final String COMPRADOR = "COMPRADOR";
    public static final String ADMIN = "ADMIN";

    /** tipo = PRODUTOR | COMPRADOR | ADMIN; id é o id na tabela correspondente. */
    public record Dados(String tipo, Long id, String nome) {}

    private UsuarioAtual() {}

    public static Dados obter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            return new Dados(jwt.getClaimAsString("tipo"), Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("nome"));
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Autenticação necessária.");
    }

    public static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
