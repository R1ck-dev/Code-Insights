package com.projeto.codeinsights.domain.identity.port;

public interface EmailSenderPort {

    /**
     * Ha canal de envio configurado?
     * <p>
     * O ambiente publico atual roda sem provedor de e-mail: hospedagem em plano gratuito bloqueia as
     * portas SMTP, e os servicos transacionais gratuitos exigem dominio proprio (Brevo, Resend) ou
     * e-mail corporativo no cadastro (SMTP2GO). Em vez de fingir que enviou, a porta declara a
     * capacidade e cada caso de uso decide o que fazer sem ela — o registro ativa a conta na hora, e
     * os fluxos que dependem de um link recusam com mensagem clara em vez de sumir em silencio.
     * <p>
     * Segue o mesmo padrao de {@code AnalisadorMetricas.suporta(linguagem)}: a porta responde pelo
     * que consegue fazer e a aplicacao se adapta, sem espalhar configuracao pela camada de aplicacao.
     */
    boolean habilitado();

    void enviarEmailAtivacao(String destinatario, String nome, String token);

    void enviarEmailRedefinicaoSenha(String destinatario, String nome, String token);
}
