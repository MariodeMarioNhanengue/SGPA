package com.example.demo.api.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.api.exception.ApiException;

/** Validação de uploads de imagem (tipo MIME + extensão), para não aceitar ficheiros arbitrários. */
final class ImagemUtil {

    private static final Set<String> EXTENSOES = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private ImagemUtil() {}

    static void validar(MultipartFile ficheiro) {
        if (ficheiro == null || ficheiro.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FICHEIRO_EM_FALTA", "Envie um ficheiro de imagem.");
        }
        String tipo = ficheiro.getContentType();
        String nome = ficheiro.getOriginalFilename() == null ? "" : ficheiro.getOriginalFilename().toLowerCase(Locale.ROOT);
        int ponto = nome.lastIndexOf('.');
        String extensao = ponto >= 0 ? nome.substring(ponto) : "";
        if (tipo == null || !tipo.toLowerCase(Locale.ROOT).startsWith("image/") || !EXTENSOES.contains(extensao)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGEM_INVALIDA",
                "Só são aceites imagens JPG, PNG, WEBP ou GIF.");
        }
    }
}
