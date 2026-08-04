package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.projeto.codeinsights.infrastructure.metrica.c.ProgramaDeC.FuncaoDeC;

/**
 * Monta a arvore estrutural de um fonte C por descida recursiva sobre a saida do {@link LexerDeC}.
 * <p>
 * <b>Por que nao uma gramatica completa de C.</b> Reconhecer C de verdade exige resolver
 * <i>typedef</i> durante o parse (o classico <i>lexer hack</i>: {@code T * x;} e declaracao se
 * {@code T} for um tipo, e multiplicacao se nao for) e depende do pre-processador ja ter rodado.
 * Nada disso serve ao modelo de custo, que precisa de cinco fatos: onde ha laco, quantas voltas ele
 * da, o que ha no corpo, quem chama quem e o que foi alocado. Uma gramatica completa cobraria o
 * preco de recusar codigo que nao compila — e o aluno cola trecho, nao arquivo compilavel.
 * <p>
 * <b>O que isto reconhece:</b> definicoes de funcao, {@code for}, {@code while}, {@code do-while},
 * {@code if}/{@code else}, {@code switch} e comandos simples. Expressoes ficam como texto.
 * <p>
 * <b>O que isto deliberadamente nao reconhece</b>, e por isso a confianca de C nunca chega a ALTA:
 * definicao de funcao no estilo K&R ({@code int f(a) int a; {}), macro que esconde um laco, e
 * funcao aninhada (extensao do GCC). Nenhum desses aparece em codigo de aluno, mas a ausencia e
 * suposicao, e suposicao nao se apresenta como medicao.
 */
public final class ParserEstruturalDeC {

    /** Palavras que podem preceder um {@code (} sem que aquilo seja uma definicao de funcao. */
    private static final Set<String> NAO_SAO_FUNCAO = Set.of(
            "if", "for", "while", "switch", "do", "else", "return", "sizeof");

    private ParserEstruturalDeC() {
    }

    public static ProgramaDeC analisar(String fonte) {
        String codigo = LexerDeC.somenteCodigo(fonte);
        if (codigo.isBlank()) {
            return ProgramaDeC.quebrado("o codigo esta vazio");
        }

        List<FuncaoDeC> funcoes = new ArrayList<>();
        int i = 0;
        while (i < codigo.length()) {
            if (codigo.charAt(i) != '{') {
                i++;
                continue;
            }
            int fim = fimDoBloco(codigo, i);
            if (fim < 0) {
                return ProgramaDeC.quebrado("ha uma chave '{' sem fechamento; a estrutura do codigo "
                        + "nao pode ser lida com seguranca");
            }
            Tentativa tentativa = tentarFuncao(codigo, i, fim);
            if (tentativa.falha() != null) {
                return ProgramaDeC.quebrado(tentativa.falha());
            }
            if (tentativa.funcao() != null) {
                funcoes.add(tentativa.funcao());
            }
            i = fim + 1;
        }

        if (funcoes.isEmpty()) {
            return ProgramaDeC.quebrado("nenhuma definicao de funcao foi reconhecida no arquivo");
        }
        return ProgramaDeC.intacto(funcoes);
    }

    /**
     * Um bloco de nivel zero e corpo de funcao quando vem logo depois do {@code )} de uma lista de
     * parametros. E a mesma regra de forma que a ciclomatica ja usa, o que mantem o numero de
     * funcoes coerente entre as duas metricas: {@code struct S {} vem depois de identificador e
     * {@code int v[] = {} vem depois de {@code =}, entao os dois ficam de fora.
     */
    private static Tentativa tentarFuncao(String codigo, int abreChave, int fechaChave) {
        int fechaParenteses = anteriorSignificativo(codigo, abreChave);
        if (fechaParenteses < 0 || codigo.charAt(fechaParenteses) != ')') {
            return Tentativa.NAO_E_FUNCAO;
        }
        int abreParenteses = inicioDaLista(codigo, fechaParenteses);
        if (abreParenteses < 0) {
            return Tentativa.NAO_E_FUNCAO;
        }
        String nome = identificadorAntesDe(codigo, abreParenteses);
        if (nome.isEmpty() || NAO_SAO_FUNCAO.contains(nome)) {
            return Tentativa.NAO_E_FUNCAO;
        }

        Leitor leitor = new Leitor(codigo.substring(abreChave + 1, fechaChave));
        NoDeC corpo = leitor.corpoDeFuncao();
        if (leitor.falha() != null) {
            return Tentativa.comFalha("na funcao `%s`, %s".formatted(nome, leitor.falha()));
        }
        String parametros = codigo.substring(abreParenteses + 1, fechaParenteses);
        return Tentativa.comFuncao(new FuncaoDeC(nome, nomesDeParametros(parametros), corpo));
    }

    /** Resultado de olhar um bloco de nivel zero: nao era funcao, era e deu certo, ou era e falhou. */
    private record Tentativa(FuncaoDeC funcao, String falha) {

        private static final Tentativa NAO_E_FUNCAO = new Tentativa(null, null);

        static Tentativa comFuncao(FuncaoDeC funcao) {
            return new Tentativa(funcao, null);
        }

        static Tentativa comFalha(String motivo) {
            return new Tentativa(null, motivo);
        }
    }

    /**
     * Nomes dos parametros. {@code (void)} e {@code ()} nao tem nenhum; nos demais, o nome e o
     * ultimo identificador de cada item — {@code const int vetor[]} da {@code vetor}, e
     * {@code int *p} da {@code p}. Parametro sem nome (prototipo como {@code int f(int)}) entra
     * como vazio para nao desalinhar a aridade.
     */
    private static List<String> nomesDeParametros(String parametros) {
        String limpo = parametros.trim();
        if (limpo.isEmpty() || limpo.equals("void")) {
            return List.of();
        }
        return dividirNoNivelZero(limpo, ',').stream().map(ParserEstruturalDeC::nomeDoParametro).toList();
    }

    private static String nomeDoParametro(String parametro) {
        String semArranjo = parametro.contains("[") ? parametro.substring(0, parametro.indexOf('[')) : parametro;
        int fim = semArranjo.length();
        while (fim > 0 && !Character.isJavaIdentifierPart(semArranjo.charAt(fim - 1))) {
            fim--;
        }
        int inicio = fim;
        while (inicio > 0 && Character.isJavaIdentifierPart(semArranjo.charAt(inicio - 1))) {
            inicio--;
        }
        return semArranjo.substring(inicio, fim);
    }

    // ---------------------------------------------------------------- varredura de caracteres

    private static int anteriorSignificativo(String codigo, int posicao) {
        int i = posicao - 1;
        while (i >= 0 && Character.isWhitespace(codigo.charAt(i))) {
            i--;
        }
        return i;
    }

    /** Casa para tras o {@code (} do {@code )} em {@code fecha}. */
    private static int inicioDaLista(String codigo, int fecha) {
        int profundidade = 0;
        for (int i = fecha; i >= 0; i--) {
            char c = codigo.charAt(i);
            if (c == ')') {
                profundidade++;
            } else if (c == '(') {
                profundidade--;
                if (profundidade == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String identificadorAntesDe(String codigo, int posicao) {
        int fim = anteriorSignificativo(codigo, posicao) + 1;
        int inicio = fim;
        while (inicio > 0 && Character.isJavaIdentifierPart(codigo.charAt(inicio - 1))) {
            inicio--;
        }
        return codigo.substring(inicio, fim);
    }

    /** Indice da {@code }} que casa com a {@code {} em {@code abre}, ou -1 se nao houver. */
    static int fimDoBloco(String codigo, int abre) {
        int profundidade = 0;
        for (int i = abre; i < codigo.length(); i++) {
            char c = codigo.charAt(i);
            if (c == '{') {
                profundidade++;
            } else if (c == '}') {
                profundidade--;
                if (profundidade == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Divide pelo separador que estiver fora de qualquer parentese, colchete ou chave. */
    static List<String> dividirNoNivelZero(String texto, char separador) {
        List<String> partes = new ArrayList<>();
        int profundidade = 0;
        int inicio = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                profundidade++;
            } else if (c == ')' || c == ']' || c == '}') {
                profundidade--;
            } else if (c == separador && profundidade == 0) {
                partes.add(texto.substring(inicio, i));
                inicio = i + 1;
            }
        }
        partes.add(texto.substring(inicio));
        return partes;
    }

    // ---------------------------------------------------------------- descida recursiva

    /**
     * Le a sequencia de comandos de um corpo. A falha fica num campo, e nao vira excecao: quem
     * chama precisa terminar dizendo "nao consegui ler esta estrutura" e seguir para as outras
     * metricas, e nao abortar a analise inteira com um stack trace.
     */
    private static final class Leitor {

        /**
         * Teto de aninhamento. A descida e recursiva ({@code bloco} ↔ {@code comando}), e sem teto
         * um arquivo com milhares de blocos encaixados — ou uma cadeia enorme de {@code else if} —
         * estourava a pilha com {@code StackOverflowError}. Sendo um {@code Error} e nao uma
         * excecao, ele passava por cima do campo {@code falha} e derrubava a analise inteira da
         * resolucao, quebrando a promessa de que estrutura ilegivel vira {@code "?"} e a
         * ciclomatica continua saindo. Codigo de aluno nao chega perto de 200 niveis.
         */
        private static final int PROFUNDIDADE_MAXIMA = 200;

        private final String texto;
        private int i;
        private int profundidade;
        private String falha;

        private Leitor(String texto) {
            this.texto = texto;
        }

        String falha() {
            return falha;
        }

        private boolean falhou() {
            return falha != null;
        }

        /** So a primeira falha e guardada: e a que explica, as seguintes sao consequencia dela. */
        private void desistir(String motivo) {
            if (falha == null) {
                falha = motivo;
            }
        }

        NoDeC corpoDeFuncao() {
            return bloco();
        }

        private NoDeC bloco() {
            List<NoDeC> filhos = new ArrayList<>();
            while (!falhou()) {
                pularEspacos();
                if (fim() || atual() == '}') {
                    break;
                }
                int antes = i;
                NoDeC no = comando();
                if (no != null) {
                    filhos.add(no);
                }
                if (i == antes) {
                    i++;
                }
            }
            return NoDeC.bloco(filhos);
        }

        private NoDeC comando() {
            pularEspacos();
            if (fim()) {
                return null;
            }
            if (profundidade >= PROFUNDIDADE_MAXIMA) {
                desistir("aninhamento acima de %d niveis; a estrutura nao foi lida"
                        .formatted(PROFUNDIDADE_MAXIMA));
                return null;
            }
            profundidade++;
            try {
                return comandoAninhado();
            } finally {
                profundidade--;
            }
        }

        private NoDeC comandoAninhado() {
            char c = atual();
            if (c == ';') {
                i++;
                return null;
            }
            if (c == '{') {
                i++;
                NoDeC interno = bloco();
                exigirFechaChave();
                return interno;
            }
            return switch (palavraAtual()) {
                case "for" -> lerPara();
                case "while" -> lerEnquanto();
                case "do" -> lerFacaEnquanto();
                case "if" -> lerCondicional();
                case "switch" -> lerEscolha();
                case "case", "default" -> lerRotuloDeCase();
                case "else" -> pularElseSolto();
                default -> ehRotuloDeGoto() ? lerRotuloDeGoto() : lerComandoSimples();
            };
        }

        /**
         * {@code repete:} antes de um comando. Sem reconhecer esta forma, o rotulo caia em
         * {@link #lerComandoSimples()}, que so para num {@code ;} de nivel zero — e engolia como
         * texto os lacos inteiros que vinham depois. Eles sumiam da arvore e a solucao saia
         * <b>O(1)</b>: o Big-O negava lacos que a ciclomatica, no mesmo arquivo, contava.
         * <p>
         * {@code goto} para sair de laco aninhado e idioma corrente em C, e a propria
         * {@link CiclomaticaDeC} ja documenta a regra de nao contar {@code goto} — ou seja, o
         * projeto sempre assumiu que ele aparece no corpus.
         */
        private boolean ehRotuloDeGoto() {
            String palavra = palavraAtual();
            if (palavra.isEmpty()) {
                return false;
            }
            int seguinte = i + palavra.length();
            while (seguinte < texto.length() && Character.isWhitespace(texto.charAt(seguinte))) {
                seguinte++;
            }
            // `x ? a : b` nunca comeca com identificador seguido de `:`, e `a::b` nao existe em C.
            return seguinte < texto.length() && texto.charAt(seguinte) == ':';
        }

        private NoDeC lerRotuloDeGoto() {
            String nome = palavraAtual();
            consumirPalavra();
            pularEspacos();
            if (!fim() && atual() == ':') {
                i++;
            }
            return NoDeC.rotulo(nome);
        }

        private NoDeC lerPara() {
            consumirPalavra();
            List<String> partes = dividirNoNivelZero(lerParenteses(), ';');
            return NoDeC.para(parte(partes, 0), parte(partes, 1), parte(partes, 2), corpoDeLaco());
        }

        private NoDeC lerEnquanto() {
            consumirPalavra();
            return NoDeC.enquanto(lerParenteses(), corpoDeLaco());
        }

        /** {@code do CORPO while (COND);} — a condicao vem depois do corpo, e o {@code ;} e obrigatorio. */
        private NoDeC lerFacaEnquanto() {
            consumirPalavra();
            NoDeC corpo = corpoDeLaco();
            pularEspacos();
            if (!palavraAtual().equals("while")) {
                desistir("um `do` sem o `while` correspondente");
                return corpo;
            }
            consumirPalavra();
            String condicao = lerParenteses();
            pularEspacos();
            if (!fim() && atual() == ';') {
                i++;
            }
            return NoDeC.facaEnquanto(condicao, corpo);
        }

        private NoDeC lerCondicional() {
            consumirPalavra();
            String condicao = lerParenteses();
            List<NoDeC> ramos = new ArrayList<>();
            ramos.add(ouVazio(comando()));
            pularEspacos();
            if (palavraAtual().equals("else")) {
                consumirPalavra();
                ramos.add(ouVazio(comando()));
            }
            return NoDeC.condicional(condicao, ramos);
        }

        private NoDeC lerEscolha() {
            consumirPalavra();
            return NoDeC.escolha(lerParenteses(), ouVazio(comando()));
        }

        /**
         * {@code case 3:} e {@code default:} nao custam nada, mas viram no: e o rotulo que marca
         * onde um {@code case} acaba e o proximo comeca. Sem essa fronteira, a contagem de
         * auto-chamadas por caminho lia o corpo do {@code switch} como sequencia reta e parava no
         * primeiro {@code return} — descartando as chamadas de todos os {@code case} seguintes, e
         * transformando recursao ramificada em recursao linear.
         */
        private NoDeC lerRotuloDeCase() {
            String palavra = palavraAtual();
            consumirPalavra();
            int profundidade = 0;
            while (!fim()) {
                char c = atual();
                if (c == '(' || c == '[') {
                    profundidade++;
                } else if (c == ')' || c == ']') {
                    profundidade--;
                } else if (c == ':' && profundidade == 0) {
                    i++;
                    return NoDeC.rotulo(palavra);
                } else if (c == ';' || c == '}') {
                    return NoDeC.rotulo(palavra);
                }
                i++;
            }
            return NoDeC.rotulo(palavra);
        }

        private NoDeC pularElseSolto() {
            consumirPalavra();
            return comando();
        }

        private NoDeC lerComandoSimples() {
            int inicio = i;
            int profundidade = 0;
            while (!fim()) {
                char c = atual();
                if (c == '(' || c == '[' || c == '{') {
                    profundidade++;
                } else if (c == ')' || c == ']') {
                    profundidade--;
                } else if (c == '}') {
                    if (profundidade == 0) {
                        break;
                    }
                    profundidade--;
                } else if (c == ';' && profundidade == 0) {
                    break;
                }
                i++;
            }
            String comando = texto.substring(inicio, i).trim();
            if (!fim() && atual() == ';') {
                i++;
            }
            return comando.isEmpty() ? null : NoDeC.comando(comando);
        }

        /** Corpo de laco: {@code while (x);} tem corpo vazio, e vazio nao e a mesma coisa que ausente. */
        private NoDeC corpoDeLaco() {
            return ouVazio(comando());
        }

        private NoDeC ouVazio(NoDeC no) {
            return no == null ? NoDeC.bloco(List.of()) : no;
        }

        private String lerParenteses() {
            pularEspacos();
            if (fim() || atual() != '(') {
                desistir("faltou o `(` de uma condicao");
                return "";
            }
            int profundidade = 0;
            int inicio = i;
            while (!fim()) {
                char c = atual();
                if (c == '(') {
                    profundidade++;
                } else if (c == ')') {
                    profundidade--;
                    if (profundidade == 0) {
                        String interno = texto.substring(inicio + 1, i);
                        i++;
                        return interno;
                    }
                }
                i++;
            }
            desistir("ha um `(` sem fechamento numa condicao");
            return "";
        }

        private void exigirFechaChave() {
            pularEspacos();
            if (fim() || atual() != '}') {
                desistir("ha uma chave `{` sem fechamento dentro de uma funcao");
                return;
            }
            i++;
        }

        private static String parte(List<String> partes, int indice) {
            return indice < partes.size() ? partes.get(indice).trim() : "";
        }

        private String palavraAtual() {
            if (fim() || !Character.isJavaIdentifierStart(atual())) {
                return "";
            }
            int fimDaPalavra = i;
            while (fimDaPalavra < texto.length() && Character.isJavaIdentifierPart(texto.charAt(fimDaPalavra))) {
                fimDaPalavra++;
            }
            return texto.substring(i, fimDaPalavra);
        }

        private void consumirPalavra() {
            i += palavraAtual().length();
        }

        private void pularEspacos() {
            while (!fim() && Character.isWhitespace(atual())) {
                i++;
            }
        }

        private boolean fim() {
            return i >= texto.length();
        }

        private char atual() {
            return texto.charAt(i);
        }
    }
}
