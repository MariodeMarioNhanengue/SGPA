package com.sgpa.mario.api.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.sgpa.mario.api.service.exception.NaoAutorizadoException;
import com.sgpa.mario.api.service.exception.RecursoNaoEncontradoException;
import com.sgpa.mario.api.service.exception.RegraNegocioException;

/**
 * Converte todas as excepções da API em respostas JSON (RFC 7807, "problem+json"):
 * {"title", "status", "detail", "codigo", ...}. Só afecta os controladores em com.example.demo.api.
 */
@RestControllerAdvice(basePackages = "com.example.demo.api")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<ProblemDetail> resposta(HttpStatus status, String titulo, String detalhe, String codigo) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setTitle(titulo);
        pd.setProperty("codigo", codigo);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> apiException(ApiException ex) {
        return resposta(ex.getStatus(), ex.getStatus().getReasonPhrase(), ex.getMessage(), ex.getCodigo());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ProblemDetail> regraNegocio(RegraNegocioException ex) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage(), "REGRA_NEGOCIO");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), "NAO_ENCONTRADO");
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    public ResponseEntity<ProblemDetail> semPermissao(NaoAutorizadoException ex) {
        return resposta(HttpStatus.FORBIDDEN, "Sem permissão", ex.getMessage(), "SEM_PERMISSAO");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> acessoNegado(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, "Sem permissão", "Não tem permissão para esta operação.", "SEM_PERMISSAO");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflito(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Conflito",
            "A operação viola uma restrição dos dados (registo duplicado ou com dados associados).", "CONFLITO");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception ex) {
        log.error("Erro inesperado na API", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
            "Ocorreu um erro inesperado. Tente novamente mais tarde.", "ERRO_INTERNO");
    }

    /** Erros de validação (@Valid): devolve o mapa campo -> mensagem em "erros". */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(fe -> erros.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Dados inválidos. Verifique os campos indicados.");
        pd.setTitle("Validação falhou");
        pd.setProperty("codigo", "VALIDACAO");
        pd.setProperty("erros", erros);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Ficheiro demasiado grande. A classe-base já trata esta excepção, por isso sobrepõe-se o
     * método em vez de declarar um novo @ExceptionHandler (que causava "Ambiguous @ExceptionHandler").
     */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
            "O ficheiro excede o tamanho máximo permitido.");
        pd.setTitle("Ficheiro demasiado grande");
        pd.setProperty("codigo", "FICHEIRO_GRANDE");
        return handleExceptionInternal(ex, pd, headers, HttpStatus.PAYLOAD_TOO_LARGE, request);
    }
}