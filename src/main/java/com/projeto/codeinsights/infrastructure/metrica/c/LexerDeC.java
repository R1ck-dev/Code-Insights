package com.projeto.codeinsights.infrastructure.metrica.c;

/**
 * Reduz um fonte C ao seu <b>codigo executavel</b>: fora comentarios, literais de texto e diretivas
 * de pre-processador. Contar palavras-chave no texto cru daria numero errado em silencio — um
 * {@code printf("if x > 0\n")} viraria um ponto de decisao, e ninguem notaria olhando o resultado.
 * <p>
 * O que sai, e por que:
 * <ul>
 *   <li><b>Comentarios</b> ({@code //} e {@code /* *&#47;}): texto, e frequentemente codigo
 *       comentado — justamente o que nao esta no fluxo de controle.</li>
 *   <li><b>Literais</b> de string e de caractere: {@code "a && b"} nao ramifica nada. Escapes sao
 *       respeitados, senao um {@code "\\"} encerraria a string cedo demais e o resto do arquivo
 *       viraria literal.</li>
 *   <li><b>Diretivas</b> ({@code #if}, {@code #define}, …): sao decisao de <i>compilacao</i>, nao de
 *       execucao. McCabe mede caminhos no fluxo do programa, e o binario ja saiu com um so lado do
 *       {@code #if}. Contar os dois inflaria a metrica de quem usa guarda de header.</li>
 * </ul>
 * Cada trecho removido vira um espaco, e nao vazio: colar o que estava em volta poderia fundir dois
 * identificadores ({@code el/}{@code *x*}{@code /se} viraria {@code else}).
 */
public final class LexerDeC {

    private LexerDeC() {
    }

    public static String somenteCodigo(String fonte) {
        if (fonte == null || fonte.isBlank()) {
            return "";
        }

        StringBuilder saida = new StringBuilder(fonte.length());
        int i = 0;
        boolean inicioDeLinha = true;

        while (i < fonte.length()) {
            char atual = fonte.charAt(i);
            char proximo = i + 1 < fonte.length() ? fonte.charAt(i + 1) : '\0';

            if (atual == '/' && proximo == '/') {
                i = ateFimDaLinha(fonte, i);
                saida.append(' ');
            } else if (atual == '/' && proximo == '*') {
                i = ateFimDoBloco(fonte, i + 2);
                saida.append(' ');
            } else if (atual == '"' || atual == '\'') {
                i = ateFimDoLiteral(fonte, i);
                saida.append(' ');
            } else if (atual == '#' && inicioDeLinha) {
                i = ateFimDaDiretiva(fonte, i);
                saida.append(' ');
            } else {
                saida.append(atual);
                i++;
            }

            if (!saida.isEmpty()) {
                char ultimo = saida.charAt(saida.length() - 1);
                inicioDeLinha = ultimo == '\n' || (inicioDeLinha && Character.isWhitespace(ultimo));
            }
        }

        return saida.toString();
    }

    private static int ateFimDaLinha(String fonte, int i) {
        while (i < fonte.length() && fonte.charAt(i) != '\n') {
            i++;
        }
        return i;
    }

    private static int ateFimDoBloco(String fonte, int i) {
        while (i + 1 < fonte.length() && !(fonte.charAt(i) == '*' && fonte.charAt(i + 1) == '/')) {
            i++;
        }
        return Math.min(i + 2, fonte.length());
    }

    /** Escape consome o proximo caractere: sem isso, {@code '\''} e {@code "\""} terminariam cedo. */
    private static int ateFimDoLiteral(String fonte, int i) {
        char aspa = fonte.charAt(i);
        i++;
        while (i < fonte.length()) {
            char c = fonte.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == aspa) {
                return i + 1;
            }
            // Literal nao fechado (codigo truncado): parar na quebra de linha evita engolir o resto.
            if (c == '\n') {
                return i;
            }
            i++;
        }
        return i;
    }

    /** Diretiva pode continuar na linha seguinte quando a linha termina em barra invertida. */
    private static int ateFimDaDiretiva(String fonte, int i) {
        while (i < fonte.length()) {
            char c = fonte.charAt(i);
            if (c == '\\' && i + 1 < fonte.length()) {
                i += 2;
                continue;
            }
            if (c == '\n') {
                return i;
            }
            i++;
        }
        return i;
    }
}
