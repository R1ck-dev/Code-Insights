package com.projeto.codeinsights.domain.pesquisa.model;

/**
 * O texto do TCLE que o participante le, junto da versao a que ele pertence.
 * <p>
 * E um valor imutavel, carregado de um recurso versionado no repositorio — nao ha CRUD de termo.
 * O texto tem de ser exatamente o que o Comite de Etica aprovou, e um campo editavel em producao
 * permitiria alterar, sem rastro, aquilo que as pessoas ja aceitaram.
 * <p>
 * A {@code versao} e a chave de tudo: o consentimento e sempre <b>a uma versao</b>. Publicar uma
 * versao nova significa que ninguem consentiu com ela ainda, e a coorte esvazia ate as pessoas
 * responderem de novo. Isso e proposital e e a razao de a versao so mudar quando o texto aprovado
 * mudar.
 *
 * @param aprovadoPeloComite configuracao explicita, e nao deducao a partir do nome da versao.
 *        Enquanto for {@code false}, a plataforma funciona inteira — da para testar o fluxo — mas
 *        diz em toda parte que o aceite nao autoriza uso em pesquisa: no aviso da tela do termo, no
 *        relatorio de qualidade e no proprio nome do arquivo exportado. Ligar isso e um ato
 *        deliberado de quem tem o parecer do CEP em maos.
 */
public record TermoDeConsentimento(String versao, String titulo, String texto,
        boolean aprovadoPeloComite) {
}
