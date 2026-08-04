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
import com.projeto.codeinsights.infrastructure.metrica.c.AnalisadorDeC;
import com.projeto.codeinsights.infrastructure.metrica.c.BigOTempoDeCAnalisador;
import com.projeto.codeinsights.infrastructure.metrica.c.CiclomaticaDeCAnalisador;
import com.projeto.codeinsights.infrastructure.metrica.c.EspacoDeCAnalisador;

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
    /**
     * Linguagem de suporte PARCIAL. Nao e C — C hoje entrega as tres —, e sim a forma como uma
     * linguagem nova entra no motor: primeiro a ciclomatica, que e contagem, depois as estimativas.
     * C++ e a proxima candidata, e reaproveitaria quase tudo do lado de C.
     */
    private static final Fake PARCIAL = new Fake(LinguagemProgramacao.CPP,
            Set.of(TipoMetrica.COMPLEXIDADE_CICLOMATICA));

    private MotorDeMetricas motor() {
        return new MotorDeMetricas(List.of(JAVA, PARCIAL));
    }

    private Resolucao resolucao(LinguagemProgramacao linguagem) {
        return new Resolucao(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "int main(void) { return 0; }", linguagem, 3, null);
    }

    @Test
    void encaminhaCadaLinguagemParaOSeuAnalisador() {
        assertThat(motor().analisar(resolucao(LinguagemProgramacao.JAVA))).hasSize(3);
        assertThat(motor().analisar(resolucao(LinguagemProgramacao.CPP))).hasSize(1);
    }

    /** Linguagem sem analisador nao e erro: e escopo conhecido, e sai como lista vazia. */
    @Test
    void linguagemSemAnalisadorDevolveListaVazia() {
        assertThat(motor().analisar(resolucao(NAO_SUPORTADA))).isEmpty();
        assertThat(motor().metricasSuportadas(NAO_SUPORTADA)).isEmpty();
        assertThat(motor().suporta(NAO_SUPORTADA)).isFalse();
    }

    /**
     * O ponto do suporte PARCIAL: uma linguagem pode ser suportada e mesmo assim nao produzir classe
     * de tempo. Um booleano por linguagem nao consegue dizer isso, e foi por isso que a porta passou
     * a responder por metrica — sem essa distincao, toda resolucao numa linguagem de suporte parcial
     * viraria "falha de analise" na tela de qualidade.
     */
    @Test
    void suporteEhParcialPorLinguagem() {
        MotorDeMetricas motor = motor();

        assertThat(motor.suporta(LinguagemProgramacao.CPP)).isTrue();
        assertThat(motor.produz(TipoMetrica.COMPLEXIDADE_CICLOMATICA, LinguagemProgramacao.CPP)).isTrue();
        assertThat(motor.produz(TipoMetrica.BIG_O_TEMPO, LinguagemProgramacao.CPP)).isFalse();
        assertThat(motor.produz(TipoMetrica.BIG_O_TEMPO, LinguagemProgramacao.JAVA)).isTrue();
    }

    /**
     * Fiacao de verdade, com os analisadores que o Spring injeta — e nao com dublês. E o teste que
     * pega o erro que os fakes nao pegam: duas linguagens reais registradas no mesmo composite, cada
     * uma respondendo pelas metricas que de fato calcula. Sem ele, so o startup da aplicacao diria
     * se o motor subiu inteiro.
     */
    @Test
    void registraOsAnalisadoresReaisDeJavaEDeC() {
        MotorDeMetricas motor = new MotorDeMetricas(List.of(
                new JavaParserAnalisadorMetricas(List.of(
                        new BigOTempoAnalisador(), new EspacoAnalisador(), new CiclomaticaAnalisador())),
                new AnalisadorDeC(List.of(
                        new BigOTempoDeCAnalisador(), new EspacoDeCAnalisador(), new CiclomaticaDeCAnalisador()))));

        Set<TipoMetrica> asTres = Set.of(TipoMetrica.BIG_O_TEMPO, TipoMetrica.COMPLEXIDADE_ESPACO,
                TipoMetrica.COMPLEXIDADE_CICLOMATICA);

        assertThat(motor.metricasSuportadas(LinguagemProgramacao.JAVA)).isEqualTo(asTres);
        assertThat(motor.metricasSuportadas(LinguagemProgramacao.C)).isEqualTo(asTres);
        assertThat(motor.analisar(resolucao(LinguagemProgramacao.C))).hasSize(3);
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
