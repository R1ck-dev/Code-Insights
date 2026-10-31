package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nomes de {@code const int X = <literal>;} — o par, em C, do {@code static final int} do Java: um
 * nome so conta como constante quando e <b>qualificado</b> ({@code const}) e inicializado por
 * <b>literal</b> inteiro. {@code int N = 100;} sem {@code const} nao entra (ninguem rastreia se
 * alguem reescreve {@code N}), e {@code const int N = n;} tampouco: o valor vem da entrada.
 * {@code volatile const int N = 5;} tambem nao: o valor pode mudar fora do programa.
 * <p>
 * Vale para global e para local; ambos caem no mesmo conjunto dos {@code #define}, e e por isso
 * que {@link ExpressoesDeC#ehConstante} e o laco limitado nao precisam saber de onde veio o nome.
 * {@code enum { N = 100 }} fica de fora, de proposito: limitacao declarada, nao esquecimento.
 * <p>
 * A leitura roda sobre o fonte sem comentarios, literais de texto e diretivas ({@link
 * LexerDeC}): uma declaracao comentada nao pode mudar a classe de complexidade do arquivo.
 * <p>
 * <b>Sombra.</b> O conjunto e uma tabela plana de nomes, sem escopo (a mesma limitacao do lado
 * Java). Para nao ler como constante um nome que outra declaracao redefine — um parametro ou local
 * {@code N} sombreando o {@code const int N} global —, o nome sai do conjunto quando aparece como
 * <b>declarador</b> em qualquer lugar que nao seja a propria declaracao const-com-literal colhida.
 * Perde-se o acerto do caso raro; nunca se inventa constante.
 * <p>
 * Ser declarador e decidido pelo que vem <i>imediatamente antes</i> do nome, sem parser de
 * declaracoes: (1) uma palavra colada ao nome ({@code myint N}, {@code struct S N}, {@code time_t N}:
 * duas palavras seguidas nunca sao uma expressao, salvo depois de {@code return}, {@code case} e
 * afins); (2) ponteiro ({@code T *N}), so quando {@code T} e tipo conhecido, porque {@code a * N}
 * e uma multiplicacao; (3) o nome logo depois de virgula numa lista cujo primeiro item comeca por
 * tipo ({@code int i, N = n}). Qualquer outro contexto ({@code i < N}, {@code v[N]}, {@code f(a, N)},
 * {@code x = N}) e leitura e nao tira o nome. Tipos conhecidos: os nativos, os {@code typedef} do
 * proprio arquivo e as etiquetas de {@code struct}/{@code union}/{@code enum}.
 */
final class ConstantesConstEmC {

    private static final String TIPO_NUMERICO =
            "(?:unsigned|signed|long|short|int|char|size_t|ssize_t|ptrdiff_t|_Bool|bool|"
                    + "u?int(?:8|16|32|64)_t)";

    /** {@code const} antes ou depois do tipo, com ou sem {@code static}. */
    private static final Pattern CONST_COM_LITERAL = Pattern.compile(
            "\\b(?:const\\s+(?:" + TIPO_NUMERICO + "\\s+)+|(?:" + TIPO_NUMERICO + "\\s+)+const\\s+)"
                    + "([A-Za-z_]\\w*)\\s*=\\s*-?\\s*[0-9]+[uUlL]*\\s*[;,]");

    private static final Pattern VOLATILE_ANTES = Pattern.compile("\\bvolatile\\s*$");

    private static final Pattern ULTIMA_PALAVRA = Pattern.compile("(\\w+)\\s*(?:\\[[^\\]]*\\])?\\s*$");

    private static final Set<String> TIPOS_NATIVOS = Set.of("int", "char", "short", "long", "unsigned", "signed",
            "float", "double", "void", "_Bool", "bool", "size_t", "ssize_t", "ptrdiff_t");

    private static final Set<String> QUALIFICADORES = Set.of("const", "volatile", "restrict", "__restrict");

    private static final Set<String> CLASSES_DE_ARMAZENAMENTO = Set.of("static", "extern", "register", "auto", "inline");

    /** Palavras que, antes de um nome, ainda sao expressao: {@code return N;}, {@code case N:}. */
    private static final Set<String> PALAVRAS_DE_EXPRESSAO = Set.of("return", "else", "case", "goto", "sizeof",
            "do", "typeof", "_Alignof", "alignof");

    private static final Set<String> ETIQUETAS = Set.of("struct", "union", "enum");

    private ConstantesConstEmC() {
    }

    static Set<String> colher(String fonteCru) {
        Set<String> nomes = new LinkedHashSet<>();
        if (fonteCru == null) {
            return nomes;
        }
        String codigo = LexerDeC.somenteCodigo(fonteCru);
        Set<Integer> colhidas = new HashSet<>();
        Matcher achado = CONST_COM_LITERAL.matcher(codigo);
        while (achado.find()) {
            if (!VOLATILE_ANTES.matcher(codigo.substring(0, achado.start())).find()) {
                nomes.add(achado.group(1));
                colhidas.add(achado.start(1));
            }
        }
        Set<String> tiposDoAluno = tiposDoAluno(codigo);
        nomes.removeIf(nome -> redeclarado(codigo, nome, colhidas, tiposDoAluno));
        return nomes;
    }

    /** O nome e declarador em alguma ocorrencia que nao seja uma das declaracoes const-com-literal colhidas? */
    private static boolean redeclarado(String codigo, String nome, Set<Integer> colhidas, Set<String> tiposDoAluno) {
        Matcher ocorrencia = Pattern.compile("\\b" + Pattern.quote(nome) + "\\b").matcher(codigo);
        while (ocorrencia.find()) {
            if (!colhidas.contains(ocorrencia.start()) && ehDeclarador(codigo, ocorrencia.start(), tiposDoAluno)) {
                return true;
            }
        }
        return false;
    }

    /** Olha para tras do nome, saltando ponteiros e qualificadores, e decide pela primeira coisa que encontra. */
    private static boolean ehDeclarador(String codigo, int posicao, Set<String> tiposDoAluno) {
        int cursor = posicao;
        int ponteiros = 0;
        while (true) {
            cursor = pularEspacosParaTras(codigo, cursor);
            if (cursor == 0) {
                return false;
            }
            char anterior = codigo.charAt(cursor - 1);
            if (anterior == '*') {
                ponteiros++;
                cursor--;
            } else if (ehParteDePalavra(anterior)) {
                int inicio = inicioDaPalavra(codigo, cursor);
                String palavra = codigo.substring(inicio, cursor);
                if (QUALIFICADORES.contains(palavra)) {
                    cursor = inicio;
                } else {
                    return palavraAntesDoNomeDeclara(codigo, palavra, inicio, ponteiros, tiposDoAluno);
                }
            } else {
                return anterior == ',' && listaDeDeclaracao(codigo, cursor - 1, tiposDoAluno);
            }
        }
    }

    private static boolean palavraAntesDoNomeDeclara(String codigo, String palavra, int inicio, int ponteiros,
            Set<String> tiposDoAluno) {
        if (!ehIdentificador(palavra)) {
            return false;
        }
        if (ponteiros == 0) {
            return !PALAVRAS_DE_EXPRESSAO.contains(palavra);
        }
        return ehTipo(palavra, tiposDoAluno) || ETIQUETAS.contains(palavraAntes(codigo, inicio));
    }

    /** {@code int i, N = n}: o nome segue uma virgula e o primeiro item da lista comeca por tipo. */
    private static boolean listaDeDeclaracao(String codigo, int virgula, Set<String> tiposDoAluno) {
        int inicio = inicioDaLista(codigo, virgula);
        int fim = primeiraVirgulaDeNivelZero(codigo, inicio, virgula);
        return comecaPorTipo(codigo.substring(inicio, fim), tiposDoAluno);
    }

    /** Onde a lista comeca: depois do ultimo ponto e virgula, chave ou parentese aberto que ninguem fechou ainda. */
    private static int inicioDaLista(String codigo, int virgula) {
        int profundidade = 0;
        for (int i = virgula - 1; i >= 0; i--) {
            char c = codigo.charAt(i);
            if (c == ')' || c == ']') {
                profundidade++;
            } else if (c == '(' || c == '[') {
                if (profundidade == 0) {
                    return i + 1;
                }
                profundidade--;
            } else if (profundidade == 0 && (c == ';' || c == '{' || c == '}')) {
                return i + 1;
            }
        }
        return 0;
    }

    private static int primeiraVirgulaDeNivelZero(String codigo, int inicio, int limite) {
        int profundidade = 0;
        for (int i = inicio; i < limite; i++) {
            char c = codigo.charAt(i);
            if (c == '(' || c == '[') {
                profundidade++;
            } else if (c == ')' || c == ']') {
                profundidade--;
            } else if (c == ',' && profundidade == 0) {
                return i;
            }
        }
        return limite;
    }

    private static boolean comecaPorTipo(String item, Set<String> tiposDoAluno) {
        Matcher palavras = Pattern.compile("\\s*([A-Za-z_]\\w*)").matcher(item);
        int cursor = 0;
        while (palavras.find(cursor) && palavras.start() == cursor) {
            String palavra = palavras.group(1);
            cursor = palavras.end();
            if (QUALIFICADORES.contains(palavra) || CLASSES_DE_ARMAZENAMENTO.contains(palavra)) {
                continue;
            }
            return !PALAVRAS_DE_EXPRESSAO.contains(palavra)
                    && (ehTipo(palavra, tiposDoAluno) || ETIQUETAS.contains(palavra) || seguidaPorPalavra(item, cursor));
        }
        return false;
    }

    private static boolean seguidaPorPalavra(String texto, int cursor) {
        int i = cursor;
        while (i < texto.length() && Character.isWhitespace(texto.charAt(i))) {
            i++;
        }
        return i < texto.length() && (Character.isLetter(texto.charAt(i)) || texto.charAt(i) == '_');
    }

    private static boolean ehTipo(String palavra, Set<String> tiposDoAluno) {
        return TIPOS_NATIVOS.contains(palavra) || palavra.matches("u?int(?:8|16|32|64)_t")
                || tiposDoAluno.contains(palavra);
    }

    /** Nomes introduzidos por {@code typedef ... NOME;} (vários declaradores e corpo de struct incluidos). */
    private static Set<String> tiposDoAluno(String codigo) {
        Set<String> tipos = new HashSet<>();
        Matcher typedef = Pattern.compile("\\btypedef\\b").matcher(codigo);
        while (typedef.find()) {
            String trecho = codigo.substring(typedef.end(), fimDoTypedef(codigo, typedef.end()));
            int corpo = trecho.lastIndexOf('}');
            for (String parte : (corpo >= 0 ? trecho.substring(corpo + 1) : trecho).split(",")) {
                Matcher nome = ULTIMA_PALAVRA.matcher(parte);
                if (nome.find()) {
                    tipos.add(nome.group(1));
                }
            }
        }
        return tipos;
    }

    private static int fimDoTypedef(String codigo, int inicio) {
        int chaves = 0;
        for (int i = inicio; i < codigo.length(); i++) {
            char c = codigo.charAt(i);
            if (c == '{') {
                chaves++;
            } else if (c == '}') {
                chaves--;
            } else if (c == ';' && chaves <= 0) {
                return i;
            }
        }
        return codigo.length();
    }

    private static String palavraAntes(String codigo, int posicao) {
        int fim = pularEspacosParaTras(codigo, posicao);
        return fim > 0 && ehParteDePalavra(codigo.charAt(fim - 1))
                ? codigo.substring(inicioDaPalavra(codigo, fim), fim)
                : "";
    }

    private static int pularEspacosParaTras(String codigo, int cursor) {
        int i = cursor;
        while (i > 0 && Character.isWhitespace(codigo.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private static int inicioDaPalavra(String codigo, int fim) {
        int i = fim;
        while (i > 0 && ehParteDePalavra(codigo.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private static boolean ehParteDePalavra(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static boolean ehIdentificador(String palavra) {
        return !palavra.isEmpty() && (Character.isLetter(palavra.charAt(0)) || palavra.charAt(0) == '_');
    }
}
