package com.sgpa.mario.api.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ContaVerificadaInterceptor contaVerificadaInterceptor;

    public WebMvcConfig(ContaVerificadaInterceptor contaVerificadaInterceptor) {
        this.contaVerificadaInterceptor = contaVerificadaInterceptor;
    }

    // Pasta base: directório de trabalho da aplicação + /uploads
    // Exemplo: /home/user/meu-projecto/uploads/
    public static final String UPLOAD_BASE;

    static {
        String base = System.getProperty("user.dir") + File.separator + "uploads";
        UPLOAD_BASE = base;
        // Criar pastas ao carregar a classe
        try {
            Files.createDirectories(Paths.get(base, "images", "perfis"));
            Files.createDirectories(Paths.get(base, "images", "ofertas"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Converter caminho para URI válido (file:///C:/... no Windows, file:///home/... no Linux)
        String location = new File(UPLOAD_BASE).toURI().toString();
        if (!location.endsWith("/")) location += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    /** Contas por validar ficam em modo restrito em toda a área de Produtor/Comprador. */
    /**
     * URLs antigos do site de login/registo/recuperação (favoritos, e-mails já enviados, etc.):
     * redirecionam para os URLs actuais em /api/v1, mantendo a query (?tipo=..., ?token=...).
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        String[][] antigos = {
            {"/login", "/api/v1/auth/login"},
            {"/logout", "/api/v1/auth/logout"},
            {"/admin/login", "/api/v1/auth/admin/login"},
            {"/admin/logout", "/api/v1/auth/admin/logout"},
            {"/recuperar-senha", "/api/v1/auth/recuperar-senha"},
            {"/redefinir-senha", "/api/v1/auth/redefinir-senha"},
            {"/confirmar-codigo", "/api/v1/auth/confirmar-email"},
            {"/comprador/criar", "/api/v1/comprador/criar"},
            {"/produtor/criar", "/api/v1/produtor/criar"},
            {"/comprador/dashboard", "/api/v1/comprador/dashboard"},
            {"/produtor/dashboard", "/api/v1/produtor/dashboard"},
            {"/admin/dashboard", "/api/v1/admin/dashboard"},
            {"/sobre", "/api/v1/sobre"},
            {"/como-funciona", "/api/v1/como-funciona"},
        };
        for (String[] r : antigos) {
            registry.addRedirectViewController(r[0], r[1]).setKeepQueryParams(true);
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(contaVerificadaInterceptor)
                .addPathPatterns("/api/v1/produtor/**", "/api/v1/comprador/**")
                // registo de novas contas continua acessível
                .excludePathPatterns("/api/v1/produtor/criar", "/api/v1/produtor/cadastrar",
                                     "/api/v1/comprador/criar", "/api/v1/comprador/cadastrar");
    }
}
