package com.sgpa.mario.api.service;

import java.util.regex.Pattern;

import com.sgpa.mario.api.service.exception.RegraNegocioException;

/**
 * Validação server-side dos dados obrigatórios no registo de Produtor e Comprador.
 *
 * A validação em JavaScript dos formulários é só conveniência (pode ser contornada,
 * e o passo 2 do formulário nem a tinha): estas regras são a garantia real e
 * aplicam-se a qualquer porta de entrada (site MVC e API REST).
 */
public final class ValidacaoRegisto {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final int SENHA_MIN = 6;
    private static final int SENHA_MAX = 72; // limite do BCrypt

    private ValidacaoRegisto() {}

    /** Texto obrigatório: devolve o valor sem espaços nas pontas ou lança erro. */
    public static String obrigatorio(String valor, String rotulo, int max) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) {
            throw new RegraNegocioException("Indique " + rotulo + ".");
        }
        if (v.length() > max) {
            throw new RegraNegocioException("O campo " + rotulo.replaceFirst("^(o|a) ", "") + " excede " + max + " caracteres.");
        }
        return v;
    }

    /** Telefone obrigatório com 9 a 15 dígitos (aceita espaços, "+" e hífens). */
    public static String telefone(String valor) {
        String v = obrigatorio(valor, "o telefone", 20);
        int digitos = v.replaceAll("\\D", "").length();
        if (digitos < 9 || digitos > 15) {
            throw new RegraNegocioException("Telefone inválido. Indique um número com pelo menos 9 dígitos.");
        }
        return v;
    }

    /** E-mail obrigatório e com formato válido — sem ele não há como confirmar a conta. */
    public static String email(String valor, int max) {
        String v = obrigatorio(valor, "o e-mail", max);
        if (!EMAIL.matcher(v).matches()) {
            throw new RegraNegocioException("E-mail inválido. Verifique se o escreveu correctamente.");
        }
        return v;
    }

    /** Senha obrigatória, entre 6 e 72 caracteres. */
    public static void senha(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("Indique a senha.");
        }
        if (valor.length() < SENHA_MIN || valor.length() > SENHA_MAX) {
            throw new RegraNegocioException("A senha deve ter entre " + SENHA_MIN + " e " + SENHA_MAX + " caracteres.");
        }
    }

    /** Confirmação de senha (só no formulário web, que tem o segundo campo). */
    public static void confirmacaoSenha(String senha, String confirmacao) {
        if (confirmacao == null || !confirmacao.equals(senha)) {
            throw new RegraNegocioException("As senhas não coincidem.");
        }
    }
}
