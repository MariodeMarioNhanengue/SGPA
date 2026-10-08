package com.sgpa.mario.api.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Respostas JSON para 401 (sem/ com token inválido) e 403 (sem permissão) geradas pelo filtro de segurança. */
@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public ApiSecurityErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escrever(response, 401, "Não autenticado", "NAO_AUTENTICADO",
                 "Token em falta, inválido ou expirado. Faça login em POST /api/v1/auth/login.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escrever(response, 403, "Sem permissão", "SEM_PERMISSAO", "Não tem permissão para esta operação.");
    }

    private void escrever(HttpServletResponse response, int status, String titulo, String codigo,
                          String detalhe) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("type", "about:blank");
        corpo.put("title", titulo);
        corpo.put("status", status);
        corpo.put("detail", detalhe);
        corpo.put("codigo", codigo);
        mapper.writeValue(response.getOutputStream(), corpo);
    }
}
