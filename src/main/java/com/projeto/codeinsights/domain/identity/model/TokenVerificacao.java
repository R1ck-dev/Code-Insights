package com.projeto.codeinsights.domain.identity.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.TipoToken;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Token de verificacao (ex.: ativacao de conta por e-mail). Expira em 24 horas.
 */
public class TokenVerificacao {

    private UUID id;
    private Usuario usuario;
    private String token;
    private LocalDateTime dataExpiracao;
    private boolean utilizado;
    private TipoToken tipo;

    /** Construtor de criacao. */
    public TokenVerificacao(Usuario usuario, TipoToken tipo) {
        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.token = UUID.randomUUID().toString();
        this.dataExpiracao = LocalDateTime.now().plusHours(24);
        this.utilizado = false;
        this.tipo = tipo;
    }

    /** Construtor de reconstituicao. */
    public TokenVerificacao(UUID id, Usuario usuario, String token, LocalDateTime dataExpiracao,
            boolean utilizado, TipoToken tipo) {
        this.id = id;
        this.usuario = usuario;
        this.token = token;
        this.dataExpiracao = dataExpiracao;
        this.utilizado = utilizado;
        this.tipo = tipo;
    }

    public void validar() {
        if (this.utilizado) {
            throw new NegocioException("Este link de verificacao ja foi utilizado.");
        }
        if (LocalDateTime.now().isAfter(this.dataExpiracao)) {
            throw new NegocioException("Este link de verificacao expirou. Solicite um novo.");
        }
    }

    public void marcarComoUtilizado() {
        this.utilizado = true;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }

    public boolean isUtilizado() {
        return utilizado;
    }

    public TipoToken getTipo() {
        return tipo;
    }
}
