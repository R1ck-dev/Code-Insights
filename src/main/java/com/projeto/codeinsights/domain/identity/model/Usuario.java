package com.projeto.codeinsights.domain.identity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.Role;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
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
    private Role role;
    private Visibilidade visibilidadePerfil;
    private StatusConta status;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;

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
        this.role = Role.ALUNO;
        this.visibilidadePerfil = Visibilidade.PRIVADO;
        this.status = StatusConta.PENDENTE_VERIFICACAO;
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    /** Construtor de reconstituicao: estado completo, usado pelo mapper ao carregar do banco. */
    public Usuario(UUID id, String username, String email, String senhaHash, Role role,
            Visibilidade visibilidadePerfil, StatusConta status, OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.senhaHash = senhaHash;
        this.role = role;
        this.visibilidadePerfil = visibilidadePerfil;
        this.status = status;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public void ativarConta() {
        this.status = StatusConta.ATIVO;
        marcarAtualizacao();
    }

    public void definirSenha(String novoHash) {
        if (novoHash == null || novoHash.isBlank()) {
            throw new NegocioException("A senha e obrigatoria.");
        }
        this.senhaHash = novoHash;
        marcarAtualizacao();
    }

    public void atualizarUsername(String novoUsername) {
        validarUsername(novoUsername);
        this.username = novoUsername.trim();
        marcarAtualizacao();
    }

    public void tornarPerfilPublico() {
        this.visibilidadePerfil = Visibilidade.PUBLICO;
        marcarAtualizacao();
    }

    public void tornarPerfilPrivado() {
        this.visibilidadePerfil = Visibilidade.PRIVADO;
        marcarAtualizacao();
    }

    public void promoverParaPesquisador() {
        this.role = Role.PESQUISADOR;
        marcarAtualizacao();
    }

    public void promoverParaAdmin() {
        this.role = Role.ADMIN;
        marcarAtualizacao();
    }

    public boolean ehAdmin() {
        return this.role == Role.ADMIN;
    }

    private void validarUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NegocioException("O nome de usuario e obrigatorio.");
        }
    }

    private void marcarAtualizacao() {
        this.atualizadoEm = OffsetDateTime.now();
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

    public Role getRole() {
        return role;
    }

    public Visibilidade getVisibilidadePerfil() {
        return visibilidadePerfil;
    }

    public StatusConta getStatus() {
        return status;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
