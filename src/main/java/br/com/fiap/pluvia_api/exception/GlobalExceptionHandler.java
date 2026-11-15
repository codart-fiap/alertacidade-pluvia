package br.com.fiap.pluvia_api.exception;

import br.com.fiap.pluvia_api.dto.ErroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Converte excecoes em respostas JSON com o status HTTP adequado.
 * Erros inesperados devolvem uma mensagem generica (sem stack trace ou detalhes internos).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhes);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> requisicaoInvalida(RequisicaoInvalidaException ex) {
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> parametrosInvalidos(Exception ex) {
        return montar(HttpStatus.BAD_REQUEST,
                "Requisição inválida: verifique os parâmetros e o corpo enviados", List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> credenciais(BadCredentialsException ex) {
        return montar(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", List.of());
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoResourceFoundException.class})
    public ResponseEntity<ErroResponse> naoEncontrado(Exception ex) {
        String mensagem = ex instanceof RecursoNaoEncontradoException
                ? ex.getMessage() : "Recurso não encontrado";
        return montar(HttpStatus.NOT_FOUND, mensagem, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return montar(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para este endereço", List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.CONFLICT,
                "Operação não permitida: o registro está em uso ou já existe", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", List.of());
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status)
                .body(new ErroResponse(status.value(), mensagem, detalhes, LocalDateTime.now()));
    }
}
