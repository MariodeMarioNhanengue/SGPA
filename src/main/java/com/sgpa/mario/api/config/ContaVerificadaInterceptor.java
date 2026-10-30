package com.sgpa.mario.api.config;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.sgpa.mario.api.service.AuthService;
import com.sgpa.mario.api.service.AuthService.ContaPorVerificar;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Modo restrito para contas por validar.
 *
 * Enquanto o e-mail de um Produtor/Comprador não for confirmado (pelo código de 6 dígitos enviado
 * por e-mail ou por um administrador), a ÚNICA coisa que a conta pode fazer é abrir o seu painel para
 * ver o aviso de validação. Qualquer outra rota de /api/v1/produtor/** e /api/v1/comprador/** — pedidos,
 * pagamentos, publicar/editar ofertas, editar perfil, trocar de perfil, etc. — é recusada
 * aqui, no servidor, por isso não há botão escondido nem URL escrito à mão que contorne.
 */
@Component
public class ContaVerificadaInterceptor implements HandlerInterceptor {

    /** Únicas páginas (GET) acessíveis a uma conta por validar. */
    private static final Set<String> PAGINAS_PERMITIDAS = Set.of("/api/v1/comprador/dashboard", "/api/v1/produtor/dashboard");

    static final String MENSAGEM =
        "Valide a sua conta para continuar. Introduza o código de 6 dígitos que lhe enviámos por e-mail "
        + "(ou peça a um administrador que a valide). Até lá não pode realizar acções na plataforma.";

    private final AuthService authService;

    public ContaVerificadaInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) return true;

        Optional<ContaPorVerificar> conta = authService.contaPorVerificar(session);
        if (conta.isEmpty()) return true;

        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        if ("GET".equalsIgnoreCase(request.getMethod()) && PAGINAS_PERMITIDAS.contains(caminho)) {
            return true;
        }

        // Pedidos feitos por JavaScript (fetch/AJAX) recebem 403 em vez de um redirect para HTML.
        String accept = request.getHeader("Accept");
        boolean pedidoDePagina = accept != null && accept.contains("text/html");
        if (!pedidoDePagina && !"GET".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, MENSAGEM);
            return false;
        }

        String destino = conta.get().tipo() == AuthService.TipoUtilizador.PRODUTOR
            ? "/api/v1/produtor/dashboard" : "/api/v1/comprador/dashboard";
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("erro", MENSAGEM);
        RequestContextUtils.saveOutputFlashMap(destino, request, response);
        response.sendRedirect(request.getContextPath() + destino);
        return false;
    }
}
