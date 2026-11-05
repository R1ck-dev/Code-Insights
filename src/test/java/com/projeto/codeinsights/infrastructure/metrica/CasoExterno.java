package com.projeto.codeinsights.infrastructure.metrica;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Um caso do corpus externo: so a anotacao, nunca o codigo. O fonte de terceiros nao tem campo
 * aqui de proposito, para que nenhum objeto que se imprime (relatorio, mensagem de erro) possa
 * carregar um trecho dele.
 * <p>
 * Cada metrica tem duas formas: o {@code Gabarito}, como o gabarito anotou, e a {@code Nota}, a
 * classe com que o motor e comparado depois da reducao do Adendo 6, item 2. As duas so diferem nos
 * casos da {@link ReducaoAoEscala}.
 *
 * @param arquivo        nome do arquivo, igual ao {@code arquivoLocal} do manifesto
 * @param ordem          prefixo numerico do arquivo (a ordem do sorteio)
 * @param problema       numero do problema na plataforma
 * @param tipoTempo      {@code pior-caso} ou {@code esperado}
 * @param espacoGabarito pode ser {@code null} se o gabarito nao anotou espaco
 */
record CasoExterno(
        String arquivo,
        LinguagemProgramacao linguagem,
        String ordem,
        String plataforma,
        String problema,
        String tempoGabarito,
        String tempoNota,
        String espacoGabarito,
        String espacoNota,
        String tipoTempo) {

    private static final String TEMPO_ESPERADO = "esperado";

    /** Identificador curto do relatorio, como o Adendo 6 cita os casos: {@code 021-3022}. */
    String id() {
        return ordem + "-" + problema;
    }

    boolean tempoNaEscala() {
        return CasoDeCorpus.estaNaEscala(tempoNota);
    }

    boolean espacoNaEscala() {
        return CasoDeCorpus.estaNaEscala(espacoNota);
    }

    boolean tempoReduzido() {
        return !tempoGabarito.equals(tempoNota);
    }

    boolean espacoReduzido() {
        return espacoGabarito != null && !espacoGabarito.equals(espacoNota);
    }

    /** Tempo esperado/amortizado em vez de pior caso (Adendo 6, item 5: conta como os demais, marcado). */
    boolean tempoEsperado() {
        return TEMPO_ESPERADO.equals(tipoTempo);
    }

    /** So metadado: arquivo e identificacao, nunca conteudo. */
    @Override
    public String toString() {
        return "%s [%s %s/%s]".formatted(arquivo, linguagem, plataforma, problema);
    }
}
