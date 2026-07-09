package com.projeto.codeinsights.infrastructure.metrica;

import org.springframework.stereotype.Component;

import com.github.javaparser.ast.CompilationUnit;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.custo.AvaliadorDeCusto;

/**
 * Estimativa da complexidade de tempo (Big O) por analise estatica da AST.
 * <p>
 * Inferir o Big O exato de codigo arbitrario e indecidivel; esta metrica e uma
 * aproximacao <b>transparente</b> - todo passo do raciocinio e gravado no
 * {@code detalhe}, e a {@code confianca} distingue o que foi deduzido do que foi
 * assumido. O objetivo e mapear a curva de amadurecimento algoritmico do aluno
 * (forca bruta -> estruturas refinadas), nao produzir um valor formalmente provado.
 * <p>
 * A inferencia vive em {@link AvaliadorDeCusto}: custo simbolico composto sobre o
 * grafo de chamadas, com lacos multiplicando o corpo, recursao resolvida como
 * relacao de recorrencia e chamadas de biblioteca tabeladas.
 */
@Component
class BigOTempoAnalisador implements AnalisadorMetricaJava {

    @Override
    public TipoMetrica tipo() {
        return TipoMetrica.BIG_O_TEMPO;
    }

    @Override
    public MetricaCalculada analisar(CompilationUnit unidade) {
        return ResumoDeComplexidade.de(AvaliadorDeCusto.doPrograma(unidade));
    }
}
