package com.projeto.codeinsights.infrastructure.config.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.TokenServicePort;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtTokenAdapter implements TokenServicePort {

    /** Tamanho minimo do segredo para HS256 (256 bits). */
    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    @Value("${api.security.jwt.secret}")
    private String secret;

    @Value("${api.security.jwt.expiration-ms}")
    private long expirationMs;

    /** Falha o boot (cedo e com mensagem clara) se o segredo for fraco para HS256. */
    @PostConstruct
    void validarSegredo() {
        if (secret == null || secret.getBytes().length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ter no minimo " + TAMANHO_MINIMO_SEGREDO_BYTES
                            + " bytes (256 bits) para assinatura HS256.");
        }
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public String gerarToken(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId().toString())
                .claim("role", "ROLE_" + usuario.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSecretKey())
                .compact();
    }

    @Override
    public String obterIdDoUsuario(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id", String.class);
    }

    @Override
    public String obterRoleDoUsuario(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
}
