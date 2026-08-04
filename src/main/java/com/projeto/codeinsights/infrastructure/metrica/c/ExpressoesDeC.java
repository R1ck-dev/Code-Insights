package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leitura das expressoes de C, que a arvore estrutural guarda como texto.
 * <p>
 * Tudo aqui responde a perguntas do modelo de custo — quem e chamado, o que foi declarado, o que e
 * constante — e nada aqui pretende entender C. Onde a resposta e ambigua, a escolha e sempre a que
 * <b>nao inventa dependencia da entrada</b>: uma expressao que nao se prova variavel e tratada como
 * variavel apenas quando ha um identificador desconhecido nela.
 */
final class ExpressoesDeC {

    /**
     * {@code #define MAX 100} — o valor precisa ser numerico; macro com expressao nao entra.
     * <p>
     * O rabo aceita comentario porque esta regex roda sobre o fonte <b>cru</b>, antes do lexer:
     * exigir fim de linha logo apos o numero fazia {@code #define MAX 100  // tamanho} nao casar, e
     * um comentario passava a mudar a classe de complexidade do arquivo.
     */
    private static final Pattern DEFINE_NUMERICO = Pattern.compile(
            "^\\s*#\\s*define\\s+([A-Za-z_]\\w*)\\s+\\(?\\s*[0-9]+[uUlL]*\\s*\\)?\\s*(?://.*|/\\*.*)?$",
            Pattern.MULTILINE);

    private static final Pattern LITERAL_INTEIRO = Pattern.compile("^[0-9]+[uUlL]*$");

    /** Palavras que podem vir antes de um {@code (} sem que aquilo seja chamada de funcao. */
    private static final Set<String> NAO_SAO_CHAMADA = Set.of(
            "if", "for", "while", "switch", "return", "sizeof", "do", "else", "case");

    /** Palavras que iniciam uma declaracao. */
    private static final Set<String> TIPOS = Set.of(
            "int", "char", "float", "double", "void", "long", "short",
            "unsigned", "signed", "const", "static", "struct", "union", "enum", "register",
            "bool", "_Bool", "size_t", "ssize_t", "ptrdiff_t", "wchar_t", "FILE", "va_list",
            "int8_t", "int16_t", "int32_t", "int64_t",
            "uint8_t", "uint16_t", "uint32_t", "uint64_t", "volatile", "extern", "inline");

    /**
     * Palavras-chave que <b>nao</b> iniciam declaracao. Servem a regra de forma
     * "identificador identificador" — sem elas, {@code return x} e {@code goto fim} passariam por
     * declaracao de um tipo chamado {@code return}.
     */
    private static final Set<String> PALAVRAS_RESERVADAS = Set.of(
            "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
            "return", "goto", "sizeof", "typedef");

    private ExpressoesDeC() {
    }

    record Chamada(String nome, String argumentos) {
    }

    /** {@code alvo operador valor}; {@code i++} chega como {@code i}, {@code +}, {@code 1}. */
    record Atribuicao(String alvo, String operador, String valor) {
    }

    // ---------------------------------------------------------------- macros

    /**
     * Constantes de {@code #define} colhidas do fonte <b>cru</b>, antes de o lexer apagar as
     * diretivas. Sem isto, o {@code MAX} de {@code #define MAX 100} / {@code int v[MAX];} seria um
     * identificador desconhecido, e todo buffer de tamanho fixo — o jeito como o aluno de C
     * declara vetor — viraria O(n) de espaco.
     */
    static Set<String> constantesDeMacro(String fonteCru) {
        Set<String> constantes = new LinkedHashSet<>();
        if (fonteCru == null) {
            return constantes;
        }
        Matcher achado = DEFINE_NUMERICO.matcher(fonteCru);
        while (achado.find()) {
            constantes.add(achado.group(1));
        }
        return constantes;
    }

    // ---------------------------------------------------------------- chamadas

    /** Chamadas de funcao no texto: um identificador colado a um {@code (}. */
    static List<Chamada> chamadasEm(String texto) {
        List<Chamada> chamadas = new ArrayList<>();
        int i = 0;
        while (i < texto.length()) {
            if (!Character.isJavaIdentifierStart(texto.charAt(i)) || ehContinuacao(texto, i)) {
                i++;
                continue;
            }
            int fimDoNome = fimDoIdentificador(texto, i);
            String nome = texto.substring(i, fimDoNome);
            int abre = proximoNaoEspaco(texto, fimDoNome);
            // TIPOS entra aqui por causa do ponteiro de funcao: em `int (*op)(int)`, o `int` vem
            // colado a um `(` e viraria uma "chamada externa `int` desconhecida" no detalhe.
            boolean naoEhChamada = NAO_SAO_CHAMADA.contains(nome) || TIPOS.contains(nome);
            if (abre < 0 || texto.charAt(abre) != '(' || naoEhChamada) {
                i = fimDoNome;
                continue;
            }
            int fecha = fimDoGrupo(texto, abre);
            if (fecha < 0) {
                break;
            }
            chamadas.add(new Chamada(nome, texto.substring(abre + 1, fecha)));
            i = abre + 1;
        }
        return chamadas;
    }

    /** Identificadores do texto, ja sem os nomes que sao chamadas de funcao. */
    static Set<String> variaveisEm(String texto) {
        Set<String> nomes = new LinkedHashSet<>();
        int i = 0;
        while (i < texto.length()) {
            if (!Character.isJavaIdentifierStart(texto.charAt(i)) || ehContinuacao(texto, i)) {
                i++;
                continue;
            }
            int fim = fimDoIdentificador(texto, i);
            int seguinte = proximoNaoEspaco(texto, fim);
            boolean ehChamada = seguinte >= 0 && texto.charAt(seguinte) == '(';
            String nome = texto.substring(i, fim);
            if (!ehChamada && !TIPOS.contains(nome) && !NAO_SAO_CHAMADA.contains(nome)) {
                nomes.add(nome);
            }
            i = fim;
        }
        return nomes;
    }

    // ---------------------------------------------------------------- declaracoes

    /**
     * Uma lista fixa de palavras-chave nao da conta: {@code bool visitado[n]} e
     * {@code No fila[n]} (typedef do proprio aluno) sao declaracoes, e ignora-las fazia o vetor
     * <b>sumir da conta de espaco</b> — trocar o tipo do vetor mudava a classe da solucao.
     * <p>
     * Por isso a segunda regra, de forma: dois identificadores seguidos, com o segundo terminando
     * em {@code [}, {@code =}, {@code ,} ou fim. Em C isso so acontece em declaracao —
     * {@code v[i] = 3} comeca com {@code [}, {@code total += i} com {@code +}, e
     * {@code printf(...)} com {@code (}.
     */
    static boolean ehDeclaracao(String comando) {
        String limpo = comando.trim();
        if (limpo.isEmpty() || !Character.isJavaIdentifierStart(limpo.charAt(0))) {
            return false;
        }
        String primeira = limpo.substring(0, fimDoIdentificador(limpo, 0));
        if (TIPOS.contains(primeira)) {
            return true;
        }
        return !PALAVRAS_RESERVADAS.contains(primeira)
                && seguidoDeDeclarador(limpo, primeira.length());
    }

    private static boolean seguidoDeDeclarador(String texto, int apos) {
        int i = apos;
        while (i < texto.length() && (Character.isWhitespace(texto.charAt(i)) || texto.charAt(i) == '*')) {
            i++;
        }
        if (i >= texto.length() || !Character.isJavaIdentifierStart(texto.charAt(i)) || i == apos) {
            return false;
        }
        int fim = fimDoIdentificador(texto, i);
        int seguinte = proximoNaoEspaco(texto, fim);
        if (seguinte < 0) {
            return true;
        }
        char c = texto.charAt(seguinte);
        return c == '[' || c == '=' || c == ',' || c == ';';
    }

    /**
     * Dimensoes de vetor que dependem da entrada, na mesma regra do lado Java: {@code int v[100]}
     * e {@code int v[MAX]} nao contam, {@code int v[n]} conta uma e {@code int m[n][n]} conta duas.
     * {@code int v[] = {...}} tem dimensao vazia, dada pelo inicializador literal, e nao conta.
     */
    static int dimensoesVariaveis(String declaracao, Set<String> constantes) {
        int maior = 0;
        for (String declarador : ParserEstruturalDeC.dividirNoNivelZero(declaracao, ',')) {
            maior = Math.max(maior, dimensoesDoDeclarador(ateOInicializador(declarador), constantes));
        }
        return maior;
    }

    /**
     * Um declarador de cada vez, e o <b>maior</b> entre eles — nunca a soma. {@code int a[n], b[n]}
     * sao dois vetores lineares, e nao uma grade {@code n x n}: somar os colchetes da linha dobrava
     * o grau e transformava O(n) em O(n^2). E o corte no inicializador tambem e por declarador,
     * senao o primeiro {@code =} da linha escondia tudo que vinha depois dele —
     * {@code int total = 0, v[n]} perdia a VLA inteira e o espaco caia para O(1).
     */
    private static int dimensoesDoDeclarador(String declarador, Set<String> constantes) {
        int variaveis = 0;
        int i = 0;
        while (i < declarador.length()) {
            if (declarador.charAt(i) != '[') {
                i++;
                continue;
            }
            int fecha = fimDoGrupo(declarador, i);
            if (fecha < 0) {
                break;
            }
            String dimensao = declarador.substring(i + 1, fecha).trim();
            if (!dimensao.isEmpty() && !ehConstante(dimensao, constantes)) {
                variaveis++;
            }
            i = fecha + 1;
        }
        return variaveis;
    }

    /**
     * So a parte que <b>declara</b>, antes do {@code =}. Sem esse corte, o colchete de uma
     * <i>leitura</i> no inicializador seria contado como dimensao reservada: {@code int soma =
     * v[i] + v[j];} viraria uma alocacao de grau 2, e uma solucao O(n) de espaco apareceria como
     * O(n^2). VLA nao aceita inicializador em C, entao nada de real se perde no corte.
     */
    private static String ateOInicializador(String declaracao) {
        int igual = posicaoDoIgual(declaracao);
        return igual < 0 ? declaracao : declaracao.substring(0, igual);
    }

    /**
     * A expressao so tem literais, constantes de macro e operadores — nao cresce com a entrada.
     * <p>
     * O operando de {@code sizeof} sai antes de olhar: {@code sizeof(struct Aluno)} e
     * {@code sizeof(No)} sao constantes de compilacao tanto quanto {@code sizeof(int)}, mas o nome
     * do tipo do aluno nao esta em lista nenhuma e seria lido como variavel — fazendo
     * {@code malloc(n * sizeof(struct Aluno))} virar O(n^2).
     */
    static boolean ehConstante(String expressao, Set<String> constantes) {
        return variaveisEm(semSizeof(expressao)).stream().allMatch(constantes::contains);
    }

    private static String semSizeof(String expressao) {
        StringBuilder limpa = new StringBuilder(expressao.length());
        int i = 0;
        while (i < expressao.length()) {
            int achado = expressao.indexOf("sizeof", i);
            if (achado < 0) {
                limpa.append(expressao, i, expressao.length());
                break;
            }
            limpa.append(expressao, i, achado);
            int abre = proximoNaoEspaco(expressao, achado + "sizeof".length());
            int fecha = abre >= 0 && expressao.charAt(abre) == '(' ? fimDoGrupo(expressao, abre) : -1;
            if (fecha < 0) {
                limpa.append(expressao, achado, expressao.length());
                break;
            }
            limpa.append('1');
            i = fecha + 1;
        }
        return limpa.toString();
    }

    /** A expressao chama alguma funcao? Um limite vindo de chamada nunca pode passar por constante. */
    static boolean temChamada(String expressao) {
        return !chamadasEm(expressao).isEmpty();
    }

    static boolean ehLiteralInteiro(String expressao) {
        return LITERAL_INTEIRO.matcher(expressao.trim()).matches();
    }

    // ---------------------------------------------------------------- atribuicoes

    /**
     * Atribuicoes e incrementos do texto. Alvos que nao sao nome simples ({@code v[i] = x}) ficam
     * de fora: o lado Java tambem so olha {@code NameExpr}, e um indice de vetor nao e o contador
     * que governa a parada de um laco.
     */
    static List<Atribuicao> atribuicoesEm(String texto) {
        List<Atribuicao> atribuicoes = new ArrayList<>(incrementosEm(texto));
        for (String parte : ParserEstruturalDeC.dividirNoNivelZero(texto, ',')) {
            atribuicaoSimples(parte).ifPresent(atribuicoes::add);
        }
        return atribuicoes;
    }

    private static Optional<Atribuicao> atribuicaoSimples(String texto) {
        int igual = posicaoDoIgual(texto);
        if (igual < 0) {
            return Optional.empty();
        }
        String esquerda = texto.substring(0, igual).trim();
        int fimDoAlvo = esquerda.length();
        while (fimDoAlvo > 0 && ehOperador(esquerda.charAt(fimDoAlvo - 1))) {
            fimDoAlvo--;
        }
        String operador = esquerda.substring(fimDoAlvo);
        String alvo = ultimaPalavra(esquerda.substring(0, fimDoAlvo).trim());
        if (!ehNomeSimples(alvo)) {
            return Optional.empty();
        }
        return Optional.of(new Atribuicao(alvo, operador, texto.substring(igual + 1).trim()));
    }

    /**
     * {@code int i = 0} declara e atribui na mesma expressao, e o alvo e o ultimo identificador
     * antes do {@code =} — o mesmo vale para {@code unsigned long total = 0}. Alvo composto
     * ({@code v[i]}, {@code *p}) nao vira nome simples e por isso e descartado adiante.
     */
    private static String ultimaPalavra(String esquerda) {
        int ultimoEspaco = esquerda.lastIndexOf(' ');
        return ultimoEspaco < 0 ? esquerda : esquerda.substring(ultimoEspaco + 1).trim();
    }

    /** Posicao do {@code =} de atribuicao, ignorando {@code ==}, {@code !=}, {@code <=} e {@code >=}. */
    private static int posicaoDoIgual(String texto) {
        int profundidade = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '(' || c == '[') {
                profundidade++;
            } else if (c == ')' || c == ']') {
                profundidade--;
            } else if (c == '=' && profundidade == 0 && ehIgualDeAtribuicao(texto, i)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean ehIgualDeAtribuicao(String texto, int i) {
        if (i + 1 < texto.length() && texto.charAt(i + 1) == '=') {
            return false;
        }
        if (i == 0) {
            return true;
        }
        char anterior = texto.charAt(i - 1);
        if (anterior == '=' || anterior == '!') {
            return false;
        }
        // `<=` e `>=` comparam; `<<=` e `>>=` atribuem. Rejeitar tudo que vem depois de `<` ou `>`
        // matava os dois deslocamentos, e um laco `i >>= 1` deixava de ser reconhecido como
        // logaritmico — justamente o que o javadoc desta classe promete reconhecer.
        if (anterior == '<' || anterior == '>') {
            return i >= 2 && texto.charAt(i - 2) == anterior;
        }
        return true;
    }

    private static List<Atribuicao> incrementosEm(String texto) {
        List<Atribuicao> incrementos = new ArrayList<>();
        for (int i = 0; i + 1 < texto.length(); i++) {
            char c = texto.charAt(i);
            if ((c != '+' && c != '-') || texto.charAt(i + 1) != c) {
                continue;
            }
            String antes = identificadorAntesDe(texto, i);
            String depois = identificadorDepoisDe(texto, i + 2);
            String alvo = !antes.isEmpty() ? antes : depois;
            if (!alvo.isEmpty()) {
                incrementos.add(new Atribuicao(alvo, String.valueOf(c), "1"));
            }
        }
        return incrementos;
    }

    /** Caractere que pode compor o operador de uma atribuicao composta ({@code *=}, {@code >>=}). */
    private static boolean ehOperador(char c) {
        return "+-*/%&|^<>".indexOf(c) >= 0;
    }

    static boolean ehNomeSimples(String texto) {
        if (texto.isEmpty() || !Character.isJavaIdentifierStart(texto.charAt(0))) {
            return false;
        }
        return texto.chars().allMatch(Character::isJavaIdentifierPart);
    }

    // ---------------------------------------------------------------- varredura de baixo nivel

    private static boolean ehContinuacao(String texto, int i) {
        return i > 0 && Character.isJavaIdentifierPart(texto.charAt(i - 1));
    }

    private static int fimDoIdentificador(String texto, int inicio) {
        int fim = inicio;
        while (fim < texto.length() && Character.isJavaIdentifierPart(texto.charAt(fim))) {
            fim++;
        }
        return fim;
    }

    private static int proximoNaoEspaco(String texto, int inicio) {
        int i = inicio;
        while (i < texto.length() && Character.isWhitespace(texto.charAt(i))) {
            i++;
        }
        return i < texto.length() ? i : -1;
    }

    /** Indice do fechamento que casa com o {@code (} ou {@code [} em {@code abre}. */
    static int fimDoGrupo(String texto, int abre) {
        char abertura = texto.charAt(abre);
        char fechamento = abertura == '(' ? ')' : ']';
        int profundidade = 0;
        for (int i = abre; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == abertura) {
                profundidade++;
            } else if (c == fechamento) {
                profundidade--;
                if (profundidade == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String identificadorAntesDe(String texto, int posicao) {
        int fim = posicao;
        while (fim > 0 && Character.isWhitespace(texto.charAt(fim - 1))) {
            fim--;
        }
        int inicio = fim;
        while (inicio > 0 && Character.isJavaIdentifierPart(texto.charAt(inicio - 1))) {
            inicio--;
        }
        return texto.substring(inicio, fim);
    }

    private static String identificadorDepoisDe(String texto, int posicao) {
        int inicio = posicao;
        while (inicio < texto.length() && Character.isWhitespace(texto.charAt(inicio))) {
            inicio++;
        }
        return inicio >= texto.length() ? "" : texto.substring(inicio, fimDoIdentificador(texto, inicio));
    }
}
