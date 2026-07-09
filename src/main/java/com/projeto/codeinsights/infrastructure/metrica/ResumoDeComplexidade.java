package com.projeto.codeinsights.infrastructure.metrica;

import java.util.ArrayList;
import java.util.List;

import com.projeto.codeinsights.domain.knowledge.enums.ClasseComplexidade;
import com.projeto.codeinsights.infrastructure.metrica.custo.CustoAvaliado;

/**
 * Traduz o custo simbolico calculado pelo motor no {@code MetricaCalculada} que vai
 * para o banco: a classe de complexidade, a confianca e um {@code detalhe} auditavel,
 * que expoe a forma simbolica exata e todas as suposicoes feitas no caminho.
 */
final class ResumoDeComplexidade {

    private ResumoDeComplexidade() {
    }

    static MetricaCalculada de(CustoAvaliado avaliado) {
        ClasseComplexidade classe = avaliado.custo().classe();
        return new MetricaCalculada(classe.getOrdem(), classe.getRotulo(), detalhe(avaliado, classe),
                avaliado.confianca());
    }

    private static String detalhe(CustoAvaliado avaliado, ClasseComplexidade classe) {
        List<String> partes = new ArrayList<>();
        partes.add(resumoDoCusto(avaliado, classe));
        partes.addAll(avaliado.notas());
        return String.join("; ", partes) + ".";
    }

    private static String resumoDoCusto(CustoAvaliado avaliado, ClasseComplexidade classe) {
        if (avaliado.custo().ehDesconhecido()) {
            return "nao foi possivel classificar esta solucao";
        }
        String simbolico = "custo estimado = O(%s)".formatted(avaliado.custo().descricao());
        if (!avaliado.custo().forcadoNaEscala()) {
            return simbolico;
        }
        return simbolico + ", arredondado para cima ate %s para caber na escala".formatted(classe.getRotulo());
    }
}
