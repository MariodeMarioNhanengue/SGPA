package com.example.demo.web.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço central para criar e verificar hashes de senha (BCrypt).
 *
 * A partir de agora, NENHUMA senha nova é gravada em texto simples na
 * base de dados — apenas o hash (irreversível) gerado pelo BCrypt.
 *
 * Como as contas já existentes na base de dados foram criadas antes desta
 * alteração e ainda têm a senha em texto simples, o método {@link #matches}
 * também sabe comparar contra esse formato antigo. Assim que um utilizador
 * com senha antiga faz login com sucesso, o chamador deve substituir essa
 * senha pelo respectivo hash (ver AuthService/AdminAuthService), migrando
 * a conta automaticamente sem que o utilizador precise de fazer nada.
 */
@Service
public class PasswordEncoderService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Gera o hash BCrypt de uma senha em texto simples. Usar sempre antes de gravar na BD. */
    public String encode(String senhaEmTextoSimples) {
        return encoder.encode(senhaEmTextoSimples);
    }

    /** Indica se o valor guardado já está no formato de hash BCrypt (e não em texto simples). */
    public boolean isHash(String valorGuardado) {
        return valorGuardado != null
            && (valorGuardado.startsWith("$2a$") || valorGuardado.startsWith("$2b$") || valorGuardado.startsWith("$2y$"))
            && valorGuardado.length() == 60;
    }

    /**
     * Verifica se a senha fornecida corresponde ao valor guardado — seja este
     * já um hash BCrypt, seja ainda uma senha antiga em texto simples (conta
     * por migrar).
     */
    public boolean matches(String senhaFornecida, String valorGuardado) {
        if (senhaFornecida == null || valorGuardado == null) return false;
        if (isHash(valorGuardado)) {
            return encoder.matches(senhaFornecida, valorGuardado);
        }
        // Conta antiga, ainda com a senha em texto simples.
        return valorGuardado.equals(senhaFornecida);
    }
}
