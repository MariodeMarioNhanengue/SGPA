package com.sgpa.mario.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sgpa.mario.api.config.WebMvcConfig;

/**
 * Regra de negócio de armazenamento de ficheiros enviados pelo utilizador
 * (fotos de perfil e imagens de ofertas).
 */
@Service
public class ArmazenamentoService {

    private static final String DIR_PERFIS =
        WebMvcConfig.UPLOAD_BASE + java.io.File.separator + "images" + java.io.File.separator + "perfis";

    private static final String DIR_OFERTAS =
        WebMvcConfig.UPLOAD_BASE + java.io.File.separator + "images" + java.io.File.separator + "ofertas";

    /** Guarda a foto de perfil de um utilizador (prefixo indica o tipo: admin, produtor, comprador). */
    public String guardarFotoPerfil(MultipartFile ficheiro, String prefixo, Long idUtilizador) {
        return guardar(ficheiro, DIR_PERFIS, prefixo + "_" + idUtilizador + "_", "/uploads/images/perfis/");
    }

    /** Guarda a imagem de uma oferta. */
    public String guardarImagemOferta(MultipartFile ficheiro) {
        return guardar(ficheiro, DIR_OFERTAS, "", "/uploads/images/ofertas/");
    }

    private String guardar(MultipartFile ficheiro, String diretorioDestino, String prefixoNome, String urlPublicaBase) {
        if (ficheiro == null || ficheiro.isEmpty()) return null;
        try {
            String original = ficheiro.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf(".")) : ".jpg";
            String filename = prefixoNome + UUID.randomUUID() + ext;
            Path uploadPath = Paths.get(diretorioDestino);
            Files.createDirectories(uploadPath);
            Files.copy(ficheiro.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return urlPublicaBase + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
