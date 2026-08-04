package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.AnalisadorDeLinguagem;
import com.projeto.codeinsights.infrastructure.metrica.MetricaCalculada;

import lombok.RequiredArgsConstructor;

/**
 * O analisador de C: as tres metricas do projeto, a partir de um unico fonte preparado uma vez so.
 * <p>
 * <b>Teto de confianca MEDIA, aplicado aqui e em nenhum outro lugar.</b> O motor de C le a
 * estrutura do codigo por forma — {@link ParserEstruturalDeC} nao resolve <i>typedef</i>, nao roda
 * o pre-processador e nao entende declaracao no estilo K&R. As deducoes que ele faz sobre um codigo
 * bem-comportado sao boas, mas descansam sobre uma suposicao, e suposicao nao se apresenta como
 * medicao. Concentrar o teto num ponto so e o que impede que a quarta metrica de C, escrita daqui a
 * um ano, se anuncie como ALTA por esquecimento.
 * <p>
 * <b>Consequencia que a plataforma precisa assumir de olhos abertos:</b> o filtro "somente
 * confianca ALTA" das telas de pesquisa exclui <b>toda</b> resolucao em C. Num piloto
 * majoritariamente em C, isso esvazia a amostra — as telas dizem isso explicitamente, em vez de
 * mostrarem um zero sem explicacao.
 */
@Component
@RequiredArgsConstructor
public class AnalisadorDeC implements AnalisadorDeLinguagem {

    private static final NivelConfianca TETO = NivelConfianca.MEDIA;

    private static final String MOTIVO_DO_TETO =
            "a estrutura de C e reconhecida por forma, sem parse completo da linguagem, "
                    + "entao a confianca nao passa de MEDIA";

    private final List<AnalisadorMetricaC> analisadores;

    @Override
    public LinguagemProgramacao linguagem() {
        return LinguagemProgramacao.C;
    }

    /** Derivado dos beans registrados, e nao de uma lista a parte que envelheceria em silencio. */
    @Override
    public Set<TipoMetrica> metricasSuportadas() {
        return analisadores.stream().map(AnalisadorMetricaC::tipo).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<ResultadoMetrica> analisar(Resolucao resolucao) {
        CodigoDeC codigo = CodigoDeC.de(resolucao.getCodigoFonte());
        return analisadores.stream()
                .map(analisador -> resultadoDe(analisador, analisador.analisar(codigo), resolucao))
                .toList();
    }

    private ResultadoMetrica resultadoDe(AnalisadorMetricaC analisador, MetricaCalculada calculada,
            Resolucao resolucao) {
        return new ResultadoMetrica(null, resolucao.getId(), analisador.tipo(),
                calculada.valor(), calculada.rotulo(), detalheCom(calculada), TETO.menor(calculada.confianca()));
    }

    /** So explica o rebaixamento quando ele de fato aconteceu; repetir o aviso em toda metrica seria ruido. */
    private String detalheCom(MetricaCalculada calculada) {
        if (TETO.menor(calculada.confianca()) == calculada.confianca()) {
            return calculada.detalhe();
        }
        return calculada.detalhe() + " " + MOTIVO_DO_TETO + ".";
    }
}
