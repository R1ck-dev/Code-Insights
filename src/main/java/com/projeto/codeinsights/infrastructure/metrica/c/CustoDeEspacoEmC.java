package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.projeto.codeinsights.infrastructure.metrica.c.ExpressoesDeC.Chamada;
import com.projeto.codeinsights.infrastructure.metrica.c.ProgramaDeC.FuncaoDeC;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Recursao;
import com.projeto.codeinsights.infrastructure.metrica.custo.Custo;
import com.projeto.codeinsights.infrastructure.metrica.custo.CustoAvaliado;

/**
 * Avalia a memoria de uma solucao em C: o <b>maior</b> entre o que ela reserva e a profundidade da
 * pilha de recursao — a mesma definicao do lado Java, inclusive na convencao do projeto de que
 * espaco e a memoria alocada pelo programa, <i>incluindo</i> o vetor que guarda a entrada (e nao o
 * "espaco auxiliar" da literatura, que desconta a entrada).
 * <p>
 * <b>O grau vem dos fatores da alocacao.</b> Java conta dimensoes de {@code new int[n][n]}; C
 * quase nunca tem essa forma, e sim {@code malloc(n * n)} sobre um vetor linearizado. Contar
 * colchetes daria grau 1 para uma grade {@code n x n} — subestimando, que e o erro perigoso. Por
 * isso o grau de um {@code malloc} e o numero de fatores do tamanho que dependem da entrada:
 * {@code malloc(n * sizeof(int))} da grau 1, porque {@code sizeof(int)} e constante, e
 * {@code malloc(n * n)} da grau 2. Vetor de tamanho fixo ({@code int v[100]}, {@code int v[MAX]})
 * nao cresce com a entrada e nao entra.
 */
final class CustoDeEspacoEmC {

    private CustoDeEspacoEmC() {
    }

    static CustoAvaliado doPrograma(ProgramaDeC programa, Set<String> constantes) {
        if (!programa.integro()) {
            return CustoAvaliado.desconhecido(programa.motivo());
        }
        return maiorAlocacao(programa, constantes).mais(profundidadeDaPilha(programa));
    }

    // ---------------------------------------------------------------- memoria reservada

    private static CustoAvaliado maiorAlocacao(ProgramaDeC programa, Set<String> constantes) {
        CustoAvaliado maior = CustoAvaliado.exato(Custo.CONSTANTE);
        for (FuncaoDeC funcao : programa.funcoes()) {
            for (NoDeC no : funcao.corpo().comDescendentes()) {
                maior = maior.mais(alocacaoNoTexto(no.textoProprio(), constantes));
            }
        }
        return maior;
    }

    private static CustoAvaliado alocacaoNoTexto(String texto, Set<String> constantes) {
        CustoAvaliado porDeclaracao = CustoAvaliado.exato(
                Custo.poliLog(dimensoesDeclaradas(texto, constantes), 0));
        CustoAvaliado porChamada = ExpressoesDeC.chamadasEm(texto).stream()
                .filter(chamada -> BibliotecaDeC.alocaMemoria(chamada.nome()))
                .map(chamada -> CustoAvaliado.exato(Custo.poliLog(fatoresVariaveis(chamada, constantes), 0)))
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
        return porDeclaracao.mais(porChamada);
    }

    /** {@code int v[n]} da 1; {@code int m[n][n]} da 2; {@code int v[100]} e {@code int v[MAX]}, 0. */
    private static int dimensoesDeclaradas(String texto, Set<String> constantes) {
        return ExpressoesDeC.ehDeclaracao(texto) ? ExpressoesDeC.dimensoesVariaveis(texto, constantes) : 0;
    }

    /**
     * Fatores do tamanho pedido que dependem da entrada. {@code realloc(p, n * sizeof(int))} tem o
     * tamanho no segundo argumento; {@code calloc(n, sizeof(int))} multiplica os dois. Tratar todos
     * os argumentos como fatores acerta os tres casos sem precisar saber qual funcao e qual.
     */
    private static int fatoresVariaveis(Chamada chamada, Set<String> constantes) {
        int variaveis = 0;
        for (String argumento : ParserEstruturalDeC.dividirNoNivelZero(chamada.argumentos(), ',')) {
            for (String fator : ParserEstruturalDeC.dividirNoNivelZero(argumento, '*')) {
                if (!fator.isBlank() && !ExpressoesDeC.ehConstante(fator, constantes)) {
                    variaveis++;
                }
            }
        }
        // O ponteiro reaproveitado pelo realloc e um fator falso: ele nomeia a area, nao o tamanho.
        return chamada.nome().equals("realloc") ? Math.max(0, variaveis - 1) : variaveis;
    }

    // ---------------------------------------------------------------- pilha de recursao

    /** A recorrencia diz quantos niveis a recursao desce — e, portanto, quantos quadros a pilha guarda. */
    private static CustoAvaliado profundidadeDaPilha(ProgramaDeC programa) {
        CustoAvaliado maior = CustoAvaliado.exato(Custo.CONSTANTE);
        for (FuncaoDeC funcao : programa.funcoes()) {
            if (RecursaoEmC.ehRecursiva(funcao)) {
                maior = maior.mais(custoDaPilha(RecursaoEmC.analisar(funcao)));
            }
        }
        if (!temCicloIndireto(programa)) {
            return maior;
        }
        return maior.mais(CustoAvaliado.estimado(Custo.LINEAR,
                "recursao mutua: profundidade da pilha nao derivavel de uma funcao so; assumidos n niveis"));
    }

    private static CustoAvaliado custoDaPilha(Recursao recursao) {
        return switch (recursao.reducao()) {
            case DIVISIVA -> CustoAvaliado.exato(Custo.LOGARITMICO)
                    .comNota("pilha de recursao: a divisao e conquista desce log n niveis");
            case INDETERMINADA -> CustoAvaliado.estimado(Custo.LINEAR,
                    "profundidade da pilha nao determinada; assumidos n niveis");
            default -> CustoAvaliado.exato(Custo.LINEAR).comNota("pilha de recursao: n niveis");
        };
    }

    /** Ciclo no grafo de chamadas envolvendo mais de uma funcao ({@code a} chama {@code b}, que chama {@code a}). */
    private static boolean temCicloIndireto(ProgramaDeC programa) {
        for (FuncaoDeC funcao : programa.funcoes()) {
            if (alcanca(programa, funcao.nome(), funcao.nome(), new HashSet<>(), true)) {
                return true;
            }
        }
        return false;
    }

    private static boolean alcanca(ProgramaDeC programa, String origem, String alvo,
            Set<String> visitados, boolean primeiroPasso) {
        if (!visitados.add(origem) && !primeiroPasso) {
            return false;
        }
        List<String> chamados = programa.porNome(origem)
                .map(funcao -> ExpressoesDeC.chamadasEm(funcao.corpo().textoDaSubarvore()).stream()
                        .map(Chamada::nome)
                        .filter(nome -> !nome.equals(origem))
                        .distinct()
                        .toList())
                .orElseGet(List::of);

        for (String chamado : chamados) {
            if (chamado.equals(alvo)) {
                return true;
            }
            if (programa.porNome(chamado).isPresent()
                    && alcanca(programa, chamado, alvo, visitados, false)) {
                return true;
            }
        }
        return false;
    }
}
