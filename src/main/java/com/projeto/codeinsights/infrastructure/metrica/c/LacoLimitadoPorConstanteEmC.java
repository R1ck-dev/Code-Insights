package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.projeto.codeinsights.infrastructure.metrica.c.ExpressoesDeC.Atribuicao;

/**
 * {@code for (i = 0; i < 26; i++)} nao depende da entrada: O(1). E a mesma regra do
 * {@code LacoLimitadoPorConstante} do lado Java, e nenhuma regra existe so numa linguagem.
 * <p>
 * O sinal nao e "a condicao tem uma constante" - {@code for (p = 0; trocou == 1; p++)} tem
 * constante e roda n vezes. O laco e constante so quando a variavel comparada na condicao e o
 * <b>contador do cabecalho</b>: ele nasce constante no init (declaracao ou atribuicao), e
 * comparado com um limite constante (em qualquer lado, com {@code <, <=, >, >=, !=}) e avanca no
 * passo do proprio {@code for}. Escrita no contador dentro do corpo nao e conferida.
 * <p>
 * Constante, em C, e o que {@link ExpressoesDeC#ehConstante} diz: literal, {@code -literal}, constante de
 * {@code #define}, e expressoes feitas so disso. O literal de caractere ({@code 'a'}) chega aqui
 * como {@code 0}: o lexer o troca por um marcador numerico, e a faixa {@code 'a'..'z'} e constante.
 */
final class LacoLimitadoPorConstanteEmC {

    private LacoLimitadoPorConstanteEmC() {
    }

    static boolean de(NoDeC laco, Set<String> constantes) {
        if (laco.tipo() != NoDeC.Tipo.FOR || temOperadorLogicoNoTopo(laco.condicao())) {
            return false;
        }
        return contadorComparado(laco.condicao(), constantes)
                .map(contador -> contadoresDoInicio(laco.inicio(), constantes).contains(contador)
                        && avancaNoPasso(laco.passo(), contador))
                .orElse(false);
    }

    /**
     * Uma <b>unica</b> comparacao de ordem no nivel zero da condicao. Condicao composta
     * ({@code i < n && achou == 0}) nao conta: o lado Java olha o operador de topo, e um
     * {@code &&} nao e comparacao de ordem. Varrer a condicao atras de qualquer comparacao com
     * constante fazia o {@code achou == 0} declarar o laco constante - e um laco de n voltas saia
     * como O(1), que e a direcao perigosa de errar.
     */
    private static boolean temOperadorLogicoNoTopo(String condicao) {
        int profundidade = 0;
        for (int i = 0; i + 1 < condicao.length(); i++) {
            char c = condicao.charAt(i);
            if (c == '(' || c == '[') {
                profundidade++;
            } else if (c == ')' || c == ']') {
                profundidade--;
            } else if (profundidade == 0 && (c == '&' || c == '|') && condicao.charAt(i + 1) == c) {
                return true;
            }
        }
        return false;
    }

    /** O nome de um lado da comparacao quando o outro lado e um limite constante. */
    private static Optional<String> contadorComparado(String condicao, Set<String> constantes) {
        int[] operador = comparacaoDeOrdem(condicao);
        if (operador == null) {
            return Optional.empty();
        }
        String esquerda = condicao.substring(0, operador[0]).trim();
        String direita = condicao.substring(operador[1]).trim();
        if (ehContador(esquerda, constantes) && ehLimiteConstante(direita, constantes)) {
            return Optional.of(esquerda);
        }
        if (ehContador(direita, constantes) && ehLimiteConstante(esquerda, constantes)) {
            return Optional.of(direita);
        }
        return Optional.empty();
    }

    /**
     * Inicio e fim do primeiro {@code <, <=, >, >=, !=} no nivel zero, ou {@code null}. {@code ==}
     * nao e comparacao de ordem (a flag {@code trocou == 1} e justamente o caso a recusar), e
     * {@code <<}, {@code >>} e {@code ->} nao sao comparacao.
     */
    private static int[] comparacaoDeOrdem(String condicao) {
        int profundidade = 0;
        for (int i = 0; i < condicao.length(); i++) {
            char c = condicao.charAt(i);
            char proximo = i + 1 < condicao.length() ? condicao.charAt(i + 1) : '\0';
            if (c == '(' || c == '[') {
                profundidade++;
            } else if (c == ')' || c == ']') {
                profundidade--;
            } else if (profundidade == 0 && (c == '<' || c == '>')) {
                if (proximo == c) {
                    i++;
                } else if (!(c == '>' && i > 0 && condicao.charAt(i - 1) == '-')) {
                    return new int[] {i, proximo == '=' ? i + 2 : i + 1};
                }
            } else if (profundidade == 0 && c == '!' && proximo == '=') {
                return new int[] {i, i + 2};
            }
        }
        return null;
    }

    /** Um nome simples que nao e constante de {@code #define}: uma variavel. */
    private static boolean ehContador(String lado, Set<String> constantes) {
        return ExpressoesDeC.ehNomeSimples(lado) && !constantes.contains(lado);
    }

    /**
     * Lado sem nenhuma variavel livre <b>e</b> sem chamada. A chamada precisa entrar na conta:
     * {@code i < tamanho()} nao tem identificador solto - {@code tamanho} e consumido como nome de
     * funcao - e passaria por limite constante, quando o motor nao tem como provar que o retorno
     * nao cresce com a entrada.
     */
    private static boolean ehLimiteConstante(String lado, Set<String> constantes) {
        return !lado.isEmpty()
                && !ExpressoesDeC.temChamada(lado)
                && ExpressoesDeC.ehConstante(lado, constantes);
    }

    /** Variaveis que o inicio do {@code for} faz nascer com valor constante: {@code int i = 0} ou {@code i = 0}. */
    private static Set<String> contadoresDoInicio(String inicio, Set<String> constantes) {
        Set<String> contadores = new HashSet<>();
        for (Atribuicao atribuicao : ExpressoesDeC.atribuicoesEm(inicio)) {
            if (atribuicao.operador().isEmpty() && ehLimiteConstante(atribuicao.valor(), constantes)) {
                contadores.add(atribuicao.alvo());
            }
        }
        return contadores;
    }

    /** {@code i++}, {@code --i}, {@code i += k}, {@code i *= 2}... no passo do proprio cabecalho. */
    private static boolean avancaNoPasso(String passo, String contador) {
        return ExpressoesDeC.atribuicoesEm(passo).stream().anyMatch(atribuicao -> atribuicao.alvo().equals(contador));
    }
}
