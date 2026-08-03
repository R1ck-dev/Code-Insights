package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.AnalisadorDeLinguagem;

/**
 * O analisador de C. Hoje entrega <b>apenas a ciclomatica</b>, e a lista de metricas suportadas diz
 * isso — nao ha Big-O nem complexidade de espaco para C, e a plataforma precisa afirmar a ausencia
 * em vez de deixar a resolucao parecer defeituosa.
 * <p>
 * Comecar pela ciclomatica nao e meio caminho: ela e um dos tres indicadores do projeto e o unico
 * que e <b>contagem</b>, e nao inferencia. Big-O exige o modelo de custo sobre a AST inteira e uma
 * tabela de custo da biblioteca padrao; contar pontos de decisao exige separar codigo de texto.
 * <p>
 * <b>Confianca MEDIA, e nao ALTA.</b> Do lado Java a ciclomatica sai de uma AST de verdade e nao
 * supoe nada. Aqui os pontos de decisao sao exatos, mas o P da formula — quantas funcoes ha —
 * depende de reconhecer o corpo de funcao por forma ({@code ) {} em nivel zero). Uma suposicao, por
 * mais confiavel que seja na pratica, nao pode se apresentar como medicao.
 */
@Component
public class AnalisadorDeC implements AnalisadorDeLinguagem {

    @Override
    public LinguagemProgramacao linguagem() {
        return LinguagemProgramacao.C;
    }

    @Override
    public Set<TipoMetrica> metricasSuportadas() {
        return Set.of(TipoMetrica.COMPLEXIDADE_CICLOMATICA);
    }

    @Override
    public List<ResultadoMetrica> analisar(Resolucao resolucao) {
        CiclomaticaDeC.Contagem contagem = CiclomaticaDeC.contar(resolucao.getCodigoFonte());
        int complexidade = contagem.complexidade();

        String detalhe = "%d ponto(s) de decisao em %d funcao(oes) (M = decisoes + P); "
                .formatted(contagem.decisoes(), contagem.funcoes())
                + "funcao mais ramificada = %d. ".formatted(contagem.maiorPorFuncao())
                + "As funcoes sao reconhecidas por forma, sem parse completo de C.";

        return List.of(new ResultadoMetrica(null, resolucao.getId(),
                TipoMetrica.COMPLEXIDADE_CICLOMATICA, complexidade,
                String.valueOf(complexidade), detalhe, NivelConfianca.MEDIA));
    }
}
