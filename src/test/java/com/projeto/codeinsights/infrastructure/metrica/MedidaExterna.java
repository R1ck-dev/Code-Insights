package com.projeto.codeinsights.infrastructure.metrica;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.MedidorDoCorpus.Medicao;

/**
 * O que o motor respondeu para um caso externo. Guarda so o rotulo de cada metrica, a confianca e, se
 * houve recusa, o motivo em codigo curto: nunca o {@code detalhe} da metrica (que pode citar
 * identificadores do fonte) nem o texto de uma excecao.
 * <p>
 * Recusa e a resposta {@value #INDETERMINADO} com um motivo. Ela entra no denominador e nao e acerto
 * (Adendo 6, item 5). Uma resposta {@value #INDETERMINADO} do proprio motor tem motivo {@code null}.
 *
 * @param recusaTempo   {@code null} se o motor respondeu o tempo
 * @param recusaEspaco  {@code null} se o motor respondeu o espaco
 * @param classeDaFalha nome simples da classe da excecao, so quando o motivo e {@link MotivoRecusa#FALHA}
 */
record MedidaExterna(
        CasoExterno caso,
        String tempo,
        NivelConfianca confiancaTempo,
        MotivoRecusa recusaTempo,
        String espaco,
        NivelConfianca confiancaEspaco,
        MotivoRecusa recusaEspaco,
        String classeDaFalha) {

    /** Por que o motor nao respondeu: codigo curto, sem texto de excecao. */
    enum MotivoRecusa { SEM_METRICA, FALHA, TEMPO_ESGOTADO }

    /** Como o relatorio escreve uma metrica que o motor nao respondeu (a mesma que o motor usa para "nao sei"). */
    static final String INDETERMINADO = "?";

    /** A analise terminou; metrica que o motor nao gerou de todo vira recusa {@link MotivoRecusa#SEM_METRICA}. */
    static MedidaExterna respondida(CasoExterno caso, Medicao medicao) {
        boolean semTempo = MedidorDoCorpus.AUSENTE.equals(medicao.tempo());
        boolean semEspaco = MedidorDoCorpus.AUSENTE.equals(medicao.espaco());
        return new MedidaExterna(caso,
                semTempo ? INDETERMINADO : medicao.tempo(), medicao.confiancaTempo(),
                semTempo ? MotivoRecusa.SEM_METRICA : null,
                semEspaco ? INDETERMINADO : medicao.espaco(), medicao.confiancaEspaco(),
                semEspaco ? MotivoRecusa.SEM_METRICA : null,
                null);
    }

    /** A analise nao terminou: as duas metricas viram recusa pelo mesmo motivo. */
    static MedidaExterna recusada(CasoExterno caso, MotivoRecusa motivo, String classeDaFalha) {
        return new MedidaExterna(caso,
                INDETERMINADO, NivelConfianca.BAIXA, motivo,
                INDETERMINADO, NivelConfianca.BAIXA, motivo,
                classeDaFalha);
    }

    boolean recusouTempo() {
        return recusaTempo != null;
    }

    boolean recusouEspaco() {
        return recusaEspaco != null;
    }
}
