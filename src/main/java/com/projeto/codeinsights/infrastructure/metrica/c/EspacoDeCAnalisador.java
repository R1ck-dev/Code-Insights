package com.projeto.codeinsights.infrastructure.metrica.c;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.MetricaCalculada;
import com.projeto.codeinsights.infrastructure.metrica.ResumoDeComplexidade;

/**
 * Estimativa da complexidade de espaco de uma solucao em C: o maior entre a memoria que ela reserva
 * (vetores dimensionados pela entrada, {@code malloc}, {@code calloc}) e a profundidade da pilha de
 * recursao, derivada da mesma recorrencia que o Big-O de tempo usa.
 */
@Component
public class EspacoDeCAnalisador implements AnalisadorMetricaC {

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.COMPLEXIDADE_ESPACO;
    }

    @Override
    public MetricaCalculada analisar(CodigoDeC codigo) {
        return ResumoDeComplexidade.de(CustoDeEspacoEmC.doPrograma(codigo.programa(), codigo.constantes()));
    }
}
