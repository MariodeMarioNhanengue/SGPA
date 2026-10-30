package com.sgpa.mario.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Duas cadeias de segurança:
 *  1) /api/**  -> API REST stateless, autenticação por Bearer JWT.
 *  2) restante -> site MVC (Thymeleaf) exactamente como era: livre, com a sua própria sessão.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(props.getChave()));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        return NimbusJwtDecoder.withSecretKey(props.getChave()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http, JwtDecoder decoder,
                                 JwtAuthenticationConverter converter,
                                 ApiSecurityErrorHandler erros) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // públicos: autenticação e registo
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/auth/admin/login",
                    "/api/v1/auth/confirmar-email",
                    "/api/v1/auth/reenviar-confirmacao",
                    "/api/v1/auth/recuperar-senha",
                    "/api/v1/auth/redefinir-senha",
                    "/api/v1/compradores",
                    "/api/v1/produtores").permitAll()
                // públicos: catálogo de ofertas e dados de apoio (leitura)
                .requestMatchers(HttpMethod.GET,
                    // páginas do site servidas nos mesmos URLs da API (login, registo, código e recuperação de senha)
                    "/api/v1/auth/login",
                    "/api/v1/auth/logout",
                    "/api/v1/auth/admin/login",
                    "/api/v1/auth/admin/logout",
                    "/api/v1/auth/confirmar-email",
                    "/api/v1/auth/recuperar-senha",
                    "/api/v1/auth/redefinir-senha",
                    "/api/v1/ofertas",
                    "/api/v1/ofertas/*",
                    "/api/v1/categorias",
                    "/api/v1/produtos",
                    "/api/v1/provincias",
                    "/api/v1/provincias/*/distritos",
                    "/api/v1/distritos/*/bairros",
                    // localização usada pelos formulários de registo (local.js)
                    "/api/v1/localizacao/provincias",
                    "/api/v1/localizacao/distritos/*",
                    "/api/v1/localizacao/bairros/*",
                    // páginas públicas do site
                    "/api/v1/sobre",
                    "/api/v1/como-funciona").permitAll()
                // ─── SITE (templates) ─────────────────────────────────────────────────────────────────
                // As páginas e formulários do site vivem sob /api/v1/{comprador,produtor,admin}. Aqui só se
                // deixa passar o pedido: a sessão e as permissões continuam a ser verificadas nos próprios
                // controllers do site, exactamente como antes (a antiga cadeia do site era permitAll).
                .requestMatchers("/api/v1/comprador/**").permitAll()
                .requestMatchers(
                    // lista explícita: /api/v1/produtor/ofertas e /resumo são da API e exigem JWT
                    "/api/v1/produtor/dashboard", "/api/v1/produtor/criar", "/api/v1/produtor/cadastrar",
                    "/api/v1/produtor/editar-perfil", "/api/v1/produtor/publicar-oferta",
                    "/api/v1/produtor/editar-oferta/*", "/api/v1/produtor/ofertas/*",
                    "/api/v1/produtor/minhas-ofertas", "/api/v1/produtor/pedidos", "/api/v1/produtor/historico",
                    "/api/v1/produtor/pedido/*", "/api/v1/produtor/tornar-comprador").permitAll()
                .requestMatchers(paginaAdminDoSite()).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(converter))
                .authenticationEntryPoint(erros)
                .accessDeniedHandler(erros))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(erros)
                .accessDeniedHandler(erros));
        return http.build();
    }

    /**
     * Pedido do site de administração: vem do browser (Accept: text/html), sem token JWT, para /api/v1/admin/**.
     * Qualquer outro pedido a /api/v1/admin/** (Postman, apps) continua a exigir ROLE_ADMIN, e a AdminApiController
     * exige-o também em cada método (@PreAuthorize), por isso um erro aqui nunca expõe a API.
     */
    private static RequestMatcher paginaAdminDoSite() {
        return request -> {
            String caminho = request.getRequestURI().substring(request.getContextPath().length());
            String accept = request.getHeader("Accept");
            return caminho.startsWith("/api/v1/admin/")
                && request.getHeader("Authorization") == null
                && accept != null && accept.contains("text/html");
        };
    }

    @Bean
    @Order(2)
    SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // o site MVC actual não usa tokens CSRF
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .headers(h -> h
                .cacheControl(c -> c.disable())
                .frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") List<String> origens) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(origens);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", cfg);
        return fonte;
    }
}
