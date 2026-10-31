package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Componentes fortemente conexos (SCC) de um grafo dirigido, pelo algoritmo de Tarjan.
 * <p>
 * Serve ao grafo de chamadas das duas linguagens: um SCC de dois ou mais nos e <b>recursao mutua</b>
 * ({@code par -> impar -> par}), que nao aparece como auto-chamada em nenhum no isolado. O grafo e
 * visto so por <b>indices</b> {@code 0..n-1}, porque o chamador identifica os nos por coisas que nao
 * servem de chave (em Java, {@code MethodDeclaration.equals} e estrutural; em C, a chave da funcao e
 * uma {@code String}). Quem usa mantem a tabela indice -> no.
 * <p>
 * <b>Auto-laco ({@code f -> f}).</b> E um ciclo, mas <b>nao</b> entra em {@link #ciclicos}: la o
 * significado e "recursao mutua entre dois ou mais nos", o mesmo que o motor ja usava, e a
 * auto-recursao simples continua sendo tratada por quem analisa a funcao sozinha. Quem quiser saber
 * se um no chama a si mesmo pergunta a {@link #temAutoLaco}. Dentro de {@link #de} o auto-laco e
 * inofensivo: um no que so aponta para si forma um componente de tamanho 1.
 * <p>
 * <b>Ordem de saida: quem e chamado primeiro.</b> Os componentes saem em ordem topologica inversa
 * (se A chama B e nao ha caminho de volta, o componente de B sai antes do de A) — a ordem em que o
 * custo dos chamados precisa estar pronto para o chamador. Os membros de cada componente saem em
 * ordem crescente de indice, para o resultado nao depender da ordem em que o grafo foi percorrido.
 * <p>
 * Pre-condicao: todo indice devolvido por {@code vizinhos} esta em {@code [0, quantidade)}.
 */
public final class ComponentesFortementeConexos {

    private final IntFunction<int[]> vizinhos;
    private final int[] ordemDeVisita;
    private final int[] menorAlcancavel;
    private final boolean[] naPilha;
    private final Deque<Integer> pilha = new ArrayDeque<>();
    private final List<int[]> componentes = new ArrayList<>();
    private int contador;

    private ComponentesFortementeConexos(int quantidade, IntFunction<int[]> vizinhos) {
        this.vizinhos = vizinhos;
        this.ordemDeVisita = new int[quantidade];
        this.menorAlcancavel = new int[quantidade];
        this.naPilha = new boolean[quantidade];
        Arrays.fill(ordemDeVisita, -1);
    }

    /** Todos os componentes, inclusive os de um no so, com os chamados antes dos chamadores. */
    public static List<int[]> de(int quantidade, IntFunction<int[]> vizinhos) {
        ComponentesFortementeConexos tarjan = new ComponentesFortementeConexos(quantidade, vizinhos);
        for (int no = 0; no < quantidade; no++) {
            if (tarjan.ordemDeVisita[no] < 0) {
                tarjan.visitar(no);
            }
        }
        return tarjan.componentes;
    }

    /** So os componentes de dois ou mais nos (recursao mutua), na mesma ordem de {@link #de}. */
    public static List<int[]> ciclicos(int quantidade, IntFunction<int[]> vizinhos) {
        return de(quantidade, vizinhos).stream().filter(componente -> componente.length >= 2).toList();
    }

    /** O no aponta para si mesmo? E a auto-recursao simples, que {@link #ciclicos} deixa de fora. */
    public static boolean temAutoLaco(int no, IntFunction<int[]> vizinhos) {
        return Arrays.stream(vizinhos.apply(no)).anyMatch(vizinho -> vizinho == no);
    }

    private void visitar(int no) {
        ordemDeVisita[no] = contador;
        menorAlcancavel[no] = contador;
        contador++;
        pilha.push(no);
        naPilha[no] = true;
        for (int vizinho : vizinhos.apply(no)) {
            atualizarPeloVizinho(no, vizinho);
        }
        if (menorAlcancavel[no] == ordemDeVisita[no]) {
            componentes.add(fecharComponenteDe(no));
        }
    }

    private void atualizarPeloVizinho(int no, int vizinho) {
        if (ordemDeVisita[vizinho] < 0) {
            visitar(vizinho);
            menorAlcancavel[no] = Math.min(menorAlcancavel[no], menorAlcancavel[vizinho]);
        } else if (naPilha[vizinho]) {
            menorAlcancavel[no] = Math.min(menorAlcancavel[no], ordemDeVisita[vizinho]);
        }
    }

    /** Desempilha ate a raiz do componente; o que sobra na pilha pertence a componentes ainda abertos. */
    private int[] fecharComponenteDe(int raiz) {
        List<Integer> membros = new ArrayList<>();
        int membro;
        do {
            membro = pilha.pop();
            naPilha[membro] = false;
            membros.add(membro);
        } while (membro != raiz);
        return membros.stream().mapToInt(Integer::intValue).sorted().toArray();
    }
}
