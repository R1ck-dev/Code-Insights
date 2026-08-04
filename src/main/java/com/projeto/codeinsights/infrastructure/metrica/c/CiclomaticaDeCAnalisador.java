package com.projeto.codeinsights.infrastructure.metrica.c;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.MetricaCalculada;

/**
 * Complexidade Ciclomatica de McCabe para C.
 * <p>
 * Continua contando sobre o <b>texto</b>, e nao sobre a arvore estrutural que o Big-O passou a
 * usar. Nao e descuido: a contagem atual ja esta em producao e trocar a forma de reconhecer funcao
 * mudaria o P da formula {@code M = decisoes + P} — ou seja, mudaria numeros ja gravados, sem que
 * ninguem tivesse pedido uma revisao da metrica. Contar decisoes tambem nao precisa de estrutura, e
 * assim a ciclomatica continua saindo mesmo quando o parser desiste do arquivo.
 */
@Component
public class CiclomaticaDeCAnalisador implements AnalisadorMetricaC {

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.COMPLEXIDADE_CICLOMATICA;
    }

    @Override
    public MetricaCalculada analisar(CodigoDeC codigo) {
        CiclomaticaDeC.Contagem contagem = CiclomaticaDeC.contar(codigo.fonte());
        int complexidade = contagem.complexidade();

        String detalhe = "%d ponto(s) de decisao em %d funcao(oes) (M = decisoes + P); "
                .formatted(contagem.decisoes(), contagem.funcoes())
                + "funcao mais ramificada = %d. ".formatted(contagem.maiorPorFuncao())
                + "As funcoes sao reconhecidas por forma, sem parse completo de C.";

        return new MetricaCalculada(complexidade, String.valueOf(complexidade), detalhe, NivelConfianca.MEDIA);
    }
}
