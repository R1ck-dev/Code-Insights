package com.projeto.codeinsights.domain.identity.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.RoleUsuario;
import com.projeto.codeinsights.domain.identity.enums.StatusUsuario;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Usuario da plataforma. Aggregate root do contexto {@code identity}.
 * POJO de dominio sem anotacoes: muda de estado apenas por metodos de
 * comportamento e valida suas invariantes lancando {@link NegocioException}.
 */
public class Usuario {

    private UUID id;
    private String username;
    private String email;
    private String senhaHash;
    private boolean perfilPublico;
    private StatusUsuario status;
    private RoleUsuario role;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /** Construtor de criacao: gera id, aplica defaults e valida invariantes. */
    public Usuario(UUID id, String username, String email, String senhaHash) {
        validarUsername(username);
        if (email == null || email.isBlank()) {
            throw new NegocioException("O e-mail e obrigatorio.");
        }
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new NegocioException("A senha e obrigatoria.");
        }

        this.id = (id != null) ? id : UUID.randomUUID();
        this.username = username.trim();
        this.email = email.trim().toLowerCase();
        this.senhaHash = senhaHash;
        this.perfilPublico = false;
        this.status = StatusUsuario.PENDENTE_VERIFICACAO;
        this.role = RoleUsuario.USER;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = this.dataCriacao;
    }

    /** Construtor de reconstituicao: estado completo, usado pelo mapper ao carregar do banco. */
    public Usuario(UUID id, String username, String email, String senhaHash, boolean perfilPublico,
            StatusUsuario status, RoleUsuario role, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfilPublico = perfilPublico;
        this.status = status;
        this.role = role;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    public void ativarConta() {
        this.status = StatusUsuario.ATIVO;
        marcarAtualizacao();
    }

    public void promoverParaAdmin() {
        this.role = RoleUsuario.ADMIN;
        marcarAtualizacao();
    }

    public void definirSenha(String novaSenhaHash) {
        if (novaSenhaHash == null || novaSenhaHash.isBlank()) {
            throw new NegocioException("A senha e obrigatoria.");
        }
        this.senhaHash = novaSenhaHash;
        marcarAtualizacao();
    }

    public void atualizarUsername(String novoUsername) {
        validarUsername(novoUsername);
        this.username = novoUsername.trim();
        marcarAtualizacao();
    }

    public void tornarPerfilPublico() {
        this.perfilPublico = true;
        marcarAtualizacao();
    }

    public void tornarPerfilPrivado() {
        this.perfilPublico = false;
        marcarAtualizacao();
    }

    private void validarUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NegocioException("O nome de usuario e obrigatorio.");
        }
    }

    private void marcarAtualizacao() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isPerfilPublico() {
        return perfilPublico;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public RoleUsuario getRole() {
        return role;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
