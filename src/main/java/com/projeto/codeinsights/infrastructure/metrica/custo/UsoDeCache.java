package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Objects;

/**
 * O papel que um array, mapa ou conjunto exerce numa recursao — o veredito de
 * {@link ClassificadorDeCache}.
 * <p>
 * Separa duas perguntas que a versao anterior fundia num {@code boolean memoizada}: a estrutura
 * <b>poda</b> a arvore de recursao (cada estado e resolvido uma vez, e o tempo cai para o numero de
 * estados) ou apenas <b>marca o caminho</b> e desfaz a marca na volta (o backtracking continua
 * exponencial, mas a pilha nunca repete um estado)?
 *
 * @param dimensoes quantos indices o cache tem — {@code memo[n]} da 1, {@code grade[i][j]} da 2;
 *                  os estados distintos sao {@code n^dimensoes}
 * @param forma     o idioma pelo qual a consulta ao cache protege as auto-chamadas
 */
public record UsoDeCache(Papel papel, int dimensoes, Forma forma) {

    public enum Papel {
        /** Nenhuma estrutura limita a recursao. */
        NENHUM,
        /** A consulta ao cache poda as auto-chamadas e a escrita fica: memoizacao ou visitados definitivos. */
        MEMOIZACAO,
        /** A consulta poda, mas a escrita e desfeita na volta: marca de caminho de um backtracking. */
        MARCA_DESFEITA
    }

    public enum Forma {
        NENHUMA,
        /** {@code if (memo[n] != 0) return memo[n];} antes das auto-chamadas. */
        GUARDA_QUE_RETORNA,
        /** {@code if (!memo.containsKey(n)) memo.put(n, f(n-1) + f(n-2));} */
        CALCULA_SE_AUSENTE,
        /** {@code memo[n] != 0 ? memo[n] : (memo[n] = f(n-1) + f(n-2))} */
        TERNARIO,
        /** {@code if (!vis[v]) f(v);} */
        CHECA_ANTES_DE_CHAMAR,
        /** {@code if (vis[v]) continue;} antes da auto-chamada no corpo do laco. */
        PULA_NO_LACO
    }

    public static final UsoDeCache NENHUM = new UsoDeCache(Papel.NENHUM, 0, Forma.NENHUMA);

    public UsoDeCache {
        Objects.requireNonNull(papel);
        Objects.requireNonNull(forma);
    }

    /** O tempo passa a ser estados distintos x trabalho por estado. */
    public boolean podaOTempo() {
        return papel == Papel.MEMOIZACAO;
    }

    /** Nenhum estado se repete na pilha, entao ela e limitada pelos estados do cache. */
    public boolean limitaAPilha() {
        return papel != Papel.NENHUM;
    }
}
