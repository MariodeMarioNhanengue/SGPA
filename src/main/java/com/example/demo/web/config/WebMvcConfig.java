package com.example.demo.web.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

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
}
