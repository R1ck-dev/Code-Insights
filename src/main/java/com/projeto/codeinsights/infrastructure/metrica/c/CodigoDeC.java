package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.Set;

/**
 * Um fonte C ja preparado para ser medido, feito uma vez e passado a todas as metricas — o
 * equivalente da {@code CompilationUnit} que o lado Java parseia uma vez so.
 *
 * @param fonte      o codigo como o aluno submeteu. A ciclomatica trabalha sobre ele, porque contar
 *                   pontos de decisao nao precisa de estrutura e funciona ate em codigo truncado.
 * @param programa   a arvore estrutural, de onde saem Big-O e espaco
 * @param constantes nomes de {@code #define} com valor numerico, colhidos <b>antes</b> de o lexer
 *                   apagar as diretivas — sem eles, {@code int v[MAX]} pareceria depender da entrada
 */
public record CodigoDeC(String fonte, ProgramaDeC programa, Set<String> constantes) {

    public static CodigoDeC de(String fonte) {
        String seguro = fonte == null ? "" : fonte;
        return new CodigoDeC(seguro, ParserEstruturalDeC.analisar(seguro),
                ExpressoesDeC.constantesDeMacro(seguro));
    }
}
