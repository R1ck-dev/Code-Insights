package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nomes de {@code const int X = <literal>;} — o par, em C, do {@code static final int} do Java: um
 * nome so conta como constante quando e <b>qualificado</b> ({@code const}) e inicializado por
 * <b>literal</b> inteiro. {@code int N = 100;} sem {@code const} nao entra (ninguem rastreia se
 * alguem reescreve {@code N}), e {@code const int N = n;} tampouco: o valor vem da entrada.
 * <p>
 * Vale para global e para local; ambos caem no mesmo conjunto dos {@code #define}, e e por isso
 * que {@link ExpressoesDeC#ehConstante} e o laco limitado nao precisam saber de onde veio o nome.
 * {@code enum { N = 100 }} fica de fora, de proposito: limitacao declarada, nao esquecimento.
 * <p>
 * A leitura roda sobre o fonte sem comentarios, literais de texto e diretivas ({@link
 * LexerDeC}): uma declaracao comentada nao pode mudar a classe de complexidade do arquivo.
 * <p>
 * <b>Sombra.</b> O conjunto e uma tabela plana de nomes, sem escopo (a mesma limitacao do lado
 * Java). Para nao ler como constante um nome que outra declaracao sem {@code const} redefine — um
 * parametro ou local {@code int N} sombreando o {@code const int N} global —, o nome sai do
 * conjunto quando houver <b>qualquer</b> declaracao dele que nao seja const-com-literal. Perde-se
 * o acerto do caso raro; nunca se inventa constante.
 */
final class ConstantesConstEmC {

    private static final String TIPO_NUMERICO =
            "(?:unsigned|signed|long|short|int|char|size_t|ssize_t|ptrdiff_t|_Bool|bool|"
                    + "u?int(?:8|16|32|64)_t)";

    /** {@code const} antes ou depois do tipo, com ou sem {@code static}. */
    private static final Pattern CONST_COM_LITERAL = Pattern.compile(
            "\\b(?:const\\s+(?:" + TIPO_NUMERICO + "\\s+)+|(?:" + TIPO_NUMERICO + "\\s+)+const\\s+)"
                    + "([A-Za-z_]\\w*)\\s*=\\s*-?\\s*[0-9]+[uUlL]*\\s*[;,]");

    /** Tipos que tambem sombreiam: um {@code float N} ou {@code double N} e declaracao do mesmo nome. */
    private static final String TIPO_QUALQUER = "(?:float|double|" + TIPO_NUMERICO + ")";

    private ConstantesConstEmC() {
    }

    static Set<String> colher(String fonteCru) {
        Set<String> nomes = new LinkedHashSet<>();
        if (fonteCru == null) {
            return nomes;
        }
        String codigo = LexerDeC.somenteCodigo(fonteCru);
        Matcher achado = CONST_COM_LITERAL.matcher(codigo);
        while (achado.find()) {
            nomes.add(achado.group(1));
        }
        nomes.removeIf(nome -> declaracoes(codigo, nome) != constantesDeclaradas(codigo, nome));
        return nomes;
    }

    /** Quantas vezes o nome e declarado como const com literal. */
    private static long constantesDeclaradas(String codigo, String nome) {
        Matcher achado = CONST_COM_LITERAL.matcher(codigo);
        long total = 0;
        while (achado.find()) {
            if (achado.group(1).equals(nome)) {
                total++;
            }
        }
        return total;
    }

    /** Quantas vezes o nome e declarado, com ou sem {@code const}: tipo, ponteiros, o nome. */
    private static long declaracoes(String codigo, String nome) {
        Pattern declaracao = Pattern.compile(
                "\\b" + TIPO_QUALQUER + "\\s+(?:const\\s+)?(?:\\*\\s*(?:const\\s+)?)*"
                        + Pattern.quote(nome) + "\\b");
        return declaracao.matcher(codigo).results().count();
    }
}
