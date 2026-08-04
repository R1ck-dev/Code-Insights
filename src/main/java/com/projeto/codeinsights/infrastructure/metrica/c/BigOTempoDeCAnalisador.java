package com.projeto.codeinsights.infrastructure.metrica.c;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.MetricaCalculada;
import com.projeto.codeinsights.infrastructure.metrica.ResumoDeComplexidade;

/**
 * Estimativa da complexidade de tempo de uma solucao em C, por analise estatica da arvore
 * estrutural. O raciocinio inteiro fica gravado no {@code detalhe}, como no lado Java — a
 * estimativa precisa ser auditavel pelo aluno e pelo pesquisador, e nao um veredito.
 */
@Component
public class BigOTempoDeCAnalisador implements AnalisadorMetricaC {

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.BIG_O_TEMPO;
    }

    @Override
    public MetricaCalculada analisar(CodigoDeC codigo) {
        return ResumoDeComplexidade.de(CustoDeTempoEmC.doPrograma(codigo.programa(), codigo.constantes()));
    }
}
