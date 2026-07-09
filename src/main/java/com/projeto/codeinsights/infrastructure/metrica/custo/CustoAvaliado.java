package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * Um {@link Custo} acompanhado da confianca do motor nele e das suposicoes que
 * precisou fazer para chega-lo. As {@code notas} viram o campo {@code detalhe}
 * do resultado, tornando a estimativa auditavel pelo aluno e pelo pesquisador.
 * <p>
 * A confianca so cai: compor dois custos toma a menor das duas, de modo que uma
 * unica suposicao la no fundo de um laco aninhado rebaixa o resultado inteiro.
 */
public record CustoAvaliado(Custo custo, NivelConfianca confianca, List<String> notas) {

    private static final int MAXIMO_DE_NOTAS = 8;

    public static CustoAvaliado exato(Custo custo) {
        return new CustoAvaliado(custo, NivelConfianca.ALTA, List.of());
    }

    /** Custo obtido com uma suposicao conservadora; {@code nota} explica qual. */
    public static CustoAvaliado estimado(Custo custo, String nota) {
        return new CustoAvaliado(custo, NivelConfianca.MEDIA, List.of(nota));
    }

    public static CustoAvaliado desconhecido(String nota) {
        return new CustoAvaliado(Custo.DESCONHECIDO, NivelConfianca.BAIXA, List.of(nota));
    }

    public CustoAvaliado mais(CustoAvaliado outro) {
        return combinar(custo.mais(outro.custo), outro);
    }

    public CustoAvaliado vezes(CustoAvaliado outro) {
        return combinar(custo.vezes(outro.custo), outro);
    }

    /** Substitui o custo preservando confianca e notas acumuladas. */
    public CustoAvaliado com(Custo novo) {
        return new CustoAvaliado(novo, novo.ehDesconhecido() ? NivelConfianca.BAIXA : confianca, notas);
    }

    public CustoAvaliado comNota(String nota) {
        return new CustoAvaliado(custo, confianca, unir(notas, List.of(nota)));
    }

    public CustoAvaliado rebaixado(NivelConfianca nivel) {
        return new CustoAvaliado(custo, confianca.menor(nivel), notas);
    }

    private CustoAvaliado combinar(Custo resultado, CustoAvaliado outro) {
        NivelConfianca nivel = confianca.menor(outro.confianca);
        if (resultado.ehDesconhecido()) {
            nivel = nivel.menor(NivelConfianca.BAIXA);
        }
        return new CustoAvaliado(resultado, nivel, unir(notas, outro.notas));
    }

    private static List<String> unir(List<String> primeiras, List<String> segundas) {
        Set<String> distintas = new LinkedHashSet<>(primeiras);
        distintas.addAll(segundas);
        List<String> todas = new ArrayList<>(distintas);
        return List.copyOf(todas.subList(0, Math.min(todas.size(), MAXIMO_DE_NOTAS)));
    }
}
