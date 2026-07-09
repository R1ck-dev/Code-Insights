package com.projeto.codeinsights.infrastructure.metrica;

import org.springframework.stereotype.Component;

import com.github.javaparser.ast.CompilationUnit;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.custo.AvaliadorDeEspaco;

/**
 * Estimativa da complexidade de espaco (memoria auxiliar) por analise estatica da AST:
 * o maior entre o que a solucao aloca (arrays dimensionados pela entrada, colecoes
 * dinamicas, copias) e a profundidade da pilha de recursao - esta ultima derivada da
 * mesma relacao de recorrencia usada pelo Big O de tempo.
 */
@Component
class EspacoAnalisador implements AnalisadorMetricaJava {

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.COMPLEXIDADE_ESPACO;
    }

    @Override
    public MetricaCalculada analisar(CompilationUnit unidade) {
        return ResumoDeComplexidade.de(AvaliadorDeEspaco.doPrograma(unidade));
    }
}
