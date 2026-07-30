package com.projeto.codeinsights.infrastructure.web.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Identifica o usuario alvo de uma operacao administrativa. Tem a mesma forma de
 * {@code EsqueciSenhaRequest}, mas nao o reaproveita: aquele nomeia o pedido do proprio
 * usuario que perdeu a senha, enquanto este nomeia um administrador apontando para a conta
 * de outra pessoa. Sao contratos distintos, e um deles pode mudar sem arrastar o outro.
 */
public record UsuarioPorEmailRequest(
        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "Formato de e-mail invalido.")
        String email) {
}
