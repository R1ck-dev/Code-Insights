package com.projeto.codeinsights.domain.identity.port;

public interface EmailSenderPort {
    void enviarEmailAtivacao(String destinatario, String nome, String token);

    void enviarEmailRedefinicaoSenha(String destinatario, String nome, String token);
}
