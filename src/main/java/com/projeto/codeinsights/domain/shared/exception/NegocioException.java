package com.projeto.codeinsights.domain.shared.exception;

/**
 * Excecao de regra de negocio do dominio. E lancada quando uma invariante de
 * dominio ou uma regra de aplicacao e violada. O {@code GlobalExceptionHandler}
 * a traduz para uma resposta HTTP 400 (Bad Request) em JSON.
 */
public class NegocioException extends RuntimeException {
    public NegocioException(String message) {
        super(message);
    }
}
