package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * O composite e o que torna verdadeira a promessa de "crescer por adicao" — antes dele, um segundo
 * analisador quebraria a injecao por tipo da porta. O que se testa aqui e o roteamento e as duas
 * respostas que a tela de qualidade usa para separar escopo de defeito.
 */
class MotorDeMetricasTest {

    private static final LinguagemProgramacao NAO_SUPORTADA = LinguagemProgramacao.JAVASCRIPT;

    /** Analisador de mentira: registra a linguagem e as metricas que diz saber calcular. */
    private record Fake(LinguagemProgramacao linguagem, Set<TipoMetrica> metricasSuportadas)
            implements AnalisadorDeLinguagem {

        @Override
        public List<ResultadoMetrica> analisar(Resolucao resolucao) {
            return metricasSuportadas.stream()
                    .map(tipo -> new ResultadoMetrica(null, resolucao.getId(), tipo, 1, "1", "",
                            NivelConfianca.ALTA))
                    .toList();
        }
    }

    private static final Fake JAVA = new Fake(LinguagemProgramacao.JAVA,
            Set.of(TipoMetrica.BIG_O_TEMPO, TipoMetrica.COMPLEXIDADE_ESPACO,
                    TipoMetrica.COMPLEXIDADE_CICLOMATICA));
    private static final Fake C = new Fake(LinguagemProgramacao.C,
            Set.of(TipoMetrica.COMPLEXIDADE_CICLOMATICA));

    private MotorDeMetricas motor() {
        return new MotorDeMetricas(List.of(JAVA, C));
    }

    private Resolucao resolucao(LinguagemProgramacao linguagem) {
        return new Resolucao(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "int main(void) { return 0; }", linguagem, 3, null);
    }

    @Test
    void encaminhaCadaLinguagemParaOSeuAnalisador() {
        assertThat(motor().analisar(resolucao(LinguagemProgramacao.JAVA))).hasSize(3);
        assertThat(motor().analisar(resolucao(LinguagemProgramacao.C))).hasSize(1);
    }

    /** Linguagem sem analisador nao e erro: e escopo conhecido, e sai como lista vazia. */
    @Test
    void linguagemSemAnalisadorDevolveListaVazia() {
        assertThat(motor().analisar(resolucao(NAO_SUPORTADA))).isEmpty();
        assertThat(motor().metricasSuportadas(NAO_SUPORTADA)).isEmpty();
        assertThat(motor().suporta(NAO_SUPORTADA)).isFalse();
    }

    /**
     * O ponto do suporte PARCIAL: C e suportado e mesmo assim nao produz classe de tempo. Um
     * booleano por linguagem nao consegue dizer isso, e foi por isso que a porta passou a responder
     * por metrica — sem essa distincao, toda resolucao em C viraria "falha de analise" na tela de
     * qualidade.
     */
    @Test
    void suporteEhParcialPorLinguagem() {
        MotorDeMetricas motor = motor();

        assertThat(motor.suporta(LinguagemProgramacao.C)).isTrue();
        assertThat(motor.produz(TipoMetrica.COMPLEXIDADE_CICLOMATICA, LinguagemProgramacao.C)).isTrue();
        assertThat(motor.produz(TipoMetrica.BIG_O_TEMPO, LinguagemProgramacao.C)).isFalse();
        assertThat(motor.produz(TipoMetrica.BIG_O_TEMPO, LinguagemProgramacao.JAVA)).isTrue();
    }

    /**
     * Duas implementacoes para a mesma linguagem fariam uma vencer conforme a ordem em que o Spring
     * listasse os beans, e o corpus passaria a ser medido por um motor diferente do que se pensa.
     * Falhar no startup e a unica forma de isso nao passar despercebido.
     */
    @Test
    void doisAnalisadoresParaAMesmaLinguagemDerrubamOStartup() {
        Fake outroJava = new Fake(LinguagemProgramacao.JAVA, Set.of(TipoMetrica.BIG_O_TEMPO));

        assertThatThrownBy(() -> new MotorDeMetricas(List.of(JAVA, outroJava)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("JAVA");
    }
}
