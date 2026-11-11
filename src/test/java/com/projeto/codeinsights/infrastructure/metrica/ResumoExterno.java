package com.projeto.codeinsights.infrastructure.metrica;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Direcao;

/**
 * As medidas de uma linguagem do corpus externo e a classificacao de cada uma contra a <b>nota</b>
 * (o gabarito, ou o gabarito reduzido a escala do Adendo 6, item 2), com a mesma regra de
 * {@link RelatorioDeAcuracia#direcao}: a metrica fora da escala sai do denominador principal, e
 * {@code ?} (recusa ou resposta do proprio motor) fica no denominador e nunca e acerto.
 */
final class ResumoExterno {

    private final LinguagemProgramacao linguagem;
    private final List<MedidaExterna> medidas;

    private ResumoExterno(LinguagemProgramacao linguagem, List<MedidaExterna> medidas) {
        this.linguagem = linguagem;
        this.medidas = medidas;
    }

    /** Um resumo por linguagem do corpus externo que tem casos, na ordem do gabarito. */
    static List<ResumoExterno> porLinguagem(List<MedidaExterna> medidas) {
        List<ResumoExterno> resumos = new ArrayList<>();
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            List<MedidaExterna> daLinguagem = medidas.stream().filter(m -> m.caso().linguagem() == linguagem).toList();
            if (!daLinguagem.isEmpty()) {
                resumos.add(new ResumoExterno(linguagem, daLinguagem));
            }
        }
        return resumos;
    }

    // ---------------------------------------------------------------- classificacao de um caso

    static Direcao direcaoDoTempo(MedidaExterna medida) {
        return RelatorioDeAcuracia.direcao(medida.caso().tempoNota(), medida.tempo());
    }

    /** So tem sentido se {@link #espacoAnotado} for verdadeiro: espaco sem gabarito nao se classifica. */
    static Direcao direcaoDoEspaco(MedidaExterna medida) {
        return RelatorioDeAcuracia.direcao(medida.caso().espacoNota(), medida.espaco());
    }

    static boolean espacoAnotado(MedidaExterna medida) {
        return medida.caso().espacoNota() != null;
    }

    // ---------------------------------------------------------------- numeros da linguagem

    LinguagemProgramacao linguagem() {
        return linguagem;
    }

    List<MedidaExterna> medidas() {
        return medidas;
    }

    Desempenho tempo() {
        return Desempenho.doExternoTempo(medidas);
    }

    Desempenho espaco() {
        return Desempenho.doExternoEspaco(medidas);
    }

    /** Tempo com a confianca declarada pelo motor; uma resposta recusada fica em {@code BAIXA}. */
    Desempenho tempoComConfianca(NivelConfianca nivel) {
        return Desempenho.doExternoTempo(medidas.stream().filter(m -> m.confiancaTempo() == nivel).toList());
    }

    /** Os casos cujo tempo e esperado/amortizado e nao pior caso (marcados no relatorio). */
    Desempenho tempoEsperado() {
        return Desempenho.doExternoTempo(medidas.stream().filter(m -> m.caso().tempoEsperado()).toList());
    }

    long contarNoTempo(Direcao direcao) {
        return medidas.stream().filter(m -> direcaoDoTempo(m) == direcao).count();
    }

    // ---------------------------------------------------------------- linha de contagens

    /**
     * Uma linha so de numeros, para o console da medicao: casos, taxa principal do tempo e do espaco,
     * direcao do tempo, recusas por motivo. Nada do fonte nem da anotacao alem de contagens.
     */
    String linhaDeContagens() {
        StringBuilder linha = new StringBuilder();
        linha.append(linguagem).append(": ").append(medidas.size()).append(" casos");
        linha.append(" | tempo ").append(tempo().principal().texto());
        linha.append(" | espaco ").append(espaco().principal().texto());
        linha.append(" | direcao do tempo");
        for (Direcao direcao : Direcao.values()) {
            linha.append(' ').append(direcao.name()).append('=').append(contarNoTempo(direcao));
        }
        linha.append(" | recusas");
        for (MotivoRecusa motivo : MotivoRecusa.values()) {
            linha.append(' ').append(motivo.name()).append('=')
                    .append(medidas.stream().filter(m -> m.recusaTempo() == motivo).count());
        }
        return linha.toString();
    }

    // ---------------------------------------------------------------- recusas

    /** Recusas do tempo, com o motivo, entre os casos do denominador principal. */
    long recusasNoTempo(MotivoRecusa motivo) {
        return medidas.stream().filter(m -> m.caso().tempoNaEscala() && m.recusaTempo() == motivo).count();
    }

    /** O proprio motor respondeu {@code ?}, sem falha de analise, entre os casos do denominador principal. */
    long indeterminadosNoTempo() {
        return medidas.stream().filter(m -> m.caso().tempoNaEscala() && m.recusaTempo() == null
                && MedidaExterna.INDETERMINADO.equals(m.tempo())).count();
    }

    // ---------------------------------------------------------------- listas do relatorio

    /** Mensuraveis no tempo em que o motor nao deu a classe da nota. */
    List<MedidaExterna> divergentesNoTempo() {
        return filtrar(m -> m.caso().tempoNaEscala() && direcaoDoTempo(m) != Direcao.ACERTO);
    }

    List<MedidaExterna> divergentesNoEspaco() {
        return filtrar(m -> m.caso().espacoNaEscala() && direcaoDoEspaco(m) != Direcao.ACERTO);
    }

    List<MedidaExterna> foraDaEscalaNoTempo() {
        return filtrar(m -> !m.caso().tempoNaEscala());
    }

    List<MedidaExterna> foraDaEscalaNoEspaco() {
        return filtrar(m -> espacoAnotado(m) && !m.caso().espacoNaEscala());
    }

    private List<MedidaExterna> filtrar(Predicate<MedidaExterna> criterio) {
        return medidas.stream().filter(criterio).toList();
    }
}
