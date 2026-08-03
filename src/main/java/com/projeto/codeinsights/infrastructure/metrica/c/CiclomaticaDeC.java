package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.Set;

/**
 * Complexidade Ciclomatica de McCabe para C, na <b>mesma convencao</b> do analisador de Java:
 * {@code M = (pontos de decisao) + P}, com P = numero de funcoes. Manter a formula identica e o que
 * permite comparar a ciclomatica de um aluno que escreve em C com a de um que escreve em Java — se
 * as duas divergissem por convencao, a comparacao entre participantes ficaria sem sentido.
 * <p>
 * Contam como decisao: {@code if}, {@code for}, {@code while}, cada rotulo {@code case}, o ternario
 * {@code ?} e os operadores de curto-circuito {@code &&} e {@code ||} — espelhando o lado Java.
 * <p>
 * Tres decisoes que o leitor precisa conhecer:
 * <ul>
 *   <li><b>{@code do} nao e contado.</b> Um {@code do ... while (c);} tem as duas palavras, e contar
 *       ambas daria 2 para um laco so. Como todo {@code do} traz exatamente um {@code while}, contar
 *       apenas os {@code while} ja da 1 por laco — em do-while e em while comum.</li>
 *   <li><b>{@code else} e {@code default} nao contam</b>, e {@code switch} tambem nao: quem ramifica
 *       e o {@code case}. E o mesmo criterio do lado Java, onde a entrada {@code default} nao tem
 *       rotulo e por isso nao soma.</li>
 *   <li><b>{@code goto} nao conta.</b> McCabe conta nos de predicado; um salto incondicional muda o
 *       caminho sem criar escolha.</li>
 * </ul>
 */
public final class CiclomaticaDeC {

    private static final Set<String> PALAVRAS_DE_DECISAO = Set.of("if", "for", "while", "case");

    private CiclomaticaDeC() {
    }

    /**
     * @param decisoes pontos de decisao no arquivo inteiro
     * @param funcoes definicoes de funcao encontradas — o P da formula
     * @param maiorPorFuncao maior {@code decisoes + 1} entre as funcoes; o total esconde uma funcao
     *        isoladamente complexa, e e o maximo por funcao que a literatura reporta
     */
    public record Contagem(int decisoes, int funcoes, int maiorPorFuncao) {

        public int complexidade() {
            return decisoes + Math.max(funcoes, 1);
        }
    }

    public static Contagem contar(String fonte) {
        String codigo = LexerDeC.somenteCodigo(fonte);

        int decisoes = 0;
        int profundidade = 0;
        int funcoes = 0;
        int maiorPorFuncao = 0;
        int decisoesDaFuncao = 0;
        boolean dentroDeFuncao = false;
        char anterior = '\0';

        int i = 0;
        while (i < codigo.length()) {
            char c = codigo.charAt(i);

            if (Character.isJavaIdentifierStart(c)) {
                int fim = i;
                while (fim < codigo.length() && Character.isJavaIdentifierPart(codigo.charAt(fim))) {
                    fim++;
                }
                if (PALAVRAS_DE_DECISAO.contains(codigo.substring(i, fim))) {
                    decisoes++;
                    decisoesDaFuncao++;
                }
                anterior = codigo.charAt(fim - 1);
                i = fim;
                continue;
            }

            char proximo = i + 1 < codigo.length() ? codigo.charAt(i + 1) : '\0';
            if ((c == '&' && proximo == '&') || (c == '|' && proximo == '|')) {
                decisoes++;
                decisoesDaFuncao++;
                anterior = c;
                i += 2;
                continue;
            }

            if (c == '?') {
                decisoes++;
                decisoesDaFuncao++;
            } else if (c == '{') {
                // Corpo de funcao: chave em nivel zero logo apos o fecha-parenteses da lista de
                // parametros. E heuristica, e e a razao da confianca MEDIA — `struct S {` vem depois
                // de um identificador e `int v[] = {` depois de `=`, entao os dois ficam de fora,
                // mas declaracao no estilo K&R (`int f(a) int a; {`) passaria batida.
                if (profundidade == 0 && anterior == ')') {
                    dentroDeFuncao = true;
                    funcoes++;
                    decisoesDaFuncao = 0;
                }
                profundidade++;
            } else if (c == '}') {
                profundidade = Math.max(0, profundidade - 1);
                if (profundidade == 0 && dentroDeFuncao) {
                    maiorPorFuncao = Math.max(maiorPorFuncao, decisoesDaFuncao + 1);
                    dentroDeFuncao = false;
                }
            }

            if (!Character.isWhitespace(c)) {
                anterior = c;
            }
            i++;
        }

        return new Contagem(decisoes, funcoes, Math.max(maiorPorFuncao, 1));
    }
}
