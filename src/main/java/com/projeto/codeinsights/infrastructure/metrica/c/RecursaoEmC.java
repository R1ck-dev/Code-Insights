package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.projeto.codeinsights.infrastructure.metrica.c.ExpressoesDeC.Atribuicao;
import com.projeto.codeinsights.infrastructure.metrica.c.ExpressoesDeC.Chamada;
import com.projeto.codeinsights.infrastructure.metrica.c.ProgramaDeC.FuncaoDeC;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Recursao;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Reducao;

/**
 * Extrai de uma funcao recursiva em C a <b>relacao de recorrencia</b> que ela descreve, para que o
 * {@code SolucionadorDeRecorrencia} — o mesmo do lado Java — a resolva.
 * <p>
 * Reusar o solucionador nao e conveniencia: e o que garante que {@code merge sort} receba
 * {@code O(n log n)} tanto em Java quanto em C, pelo mesmo Teorema Mestre e com a mesma conta. As
 * duas medidas que alimentam a recorrencia sao as mesmas do lado Java:
 * <ul>
 *   <li>quantas auto-chamadas <b>executam num mesmo caminho</b> ({@code a}) — a busca binaria
 *       recursiva tem duas ocorrencias no texto, mas so uma executa, porque estao em ramos
 *       exclusivos de um {@code if};</li>
 *   <li><b>quanto o argumento encolhe</b> a cada nivel ({@code b}).</li>
 * </ul>
 * <b>Lacuna conhecida e deliberada: nao ha deteccao de memoizacao nem de marcacao de visitados.</b>
 * O lado Java reconhece {@code if (memo[n] != VAZIO) return memo[n];} e passa a contar estados
 * distintos. Em C o cache costuma ser um vetor <i>linearizado</i> ({@code grade[lin * n + col]}), e
 * contar dimensoes por colchetes daria 1 onde os estados sao {@code n^2} — ou seja, o motor
 * <b>subestimaria</b>, que e o erro perigoso numa ferramenta de ensino. Sem essa deteccao, uma
 * inundacao recursiva e classificada como busca exponencial: erra para cima, e a divergencia fica
 * registrada no corpus em vez de escondida numa heuristica meia-boca.
 */
final class RecursaoEmC {

    private static final Pattern DIVISAO_POR_CONSTANTE = Pattern.compile("(/|>>)\\s*([0-9]+)");

    /** {@code no->esq} e {@code no.esq}: desce numa estrutura em vez de num numero. */
    private static final Pattern ACESSO_ESTRUTURAL = Pattern.compile("(->|\\.)\\s*[A-Za-z_]");

    private static final Set<String> SAIDAS = Set.of("return", "exit", "abort");

    private RecursaoEmC() {
    }

    static boolean ehRecursiva(FuncaoDeC funcao) {
        return !autoChamadas(funcao.corpo(), funcao.nome()).isEmpty();
    }

    static Recursao analisar(FuncaoDeC funcao) {
        int porCaminho = Math.max(1, contarPorCaminho(funcao.corpo(), funcao.nome()));
        return classificarReducao(funcao, porCaminho);
    }

    // ---------------------------------------------------------------- a: chamadas por caminho

    /**
     * Maximo de auto-chamadas num unico caminho. Ramos de {@code if} sao exclusivos (usa
     * {@code max}); comandos em sequencia se somam — mas so ate o primeiro que sempre sai, porque
     * o que vem depois e inalcancavel naquele caminho.
     */
    private static int contarPorCaminho(NoDeC no, String nome) {
        return switch (no.tipo()) {
            case COMANDO -> contarNoTexto(no.condicao(), nome);
            case ROTULO -> 0;
            case BLOCO -> contarBloco(no.filhos(), nome);
            case IF -> contarNoTexto(no.condicao(), nome) + maiorDosFilhos(no.filhos(), nome);
            case SWITCH -> contarNoTexto(no.condicao(), nome) + somaDoCorpoDoSwitch(no, nome);
            case FOR, WHILE, DO -> contarNoTexto(no.textoProprio(), nome) + maiorDosFilhos(no.filhos(), nome);
        };
    }

    /**
     * O corpo de um {@code switch} <b>soma</b>, e nao passa por {@link #contarBloco}.
     * <p>
     * Ler o corpo como sequencia reta fazia a contagem parar no primeiro {@code case} que retorna,
     * descartando as auto-chamadas de todos os seguintes: uma recursao ramificada por {@code switch}
     * virava recursao linear, e O(2^n) saia como O(n) — subestimando, o lado perigoso.
     * <p>
     * Somar e o que o lado Java faz (o {@code contarPorCaminho} dele nao tem ramo para
     * {@code SwitchStmt} e cai em {@code somaDosFilhos}). Superestima quando os {@code case} sao
     * de fato exclusivos, mas errar junto com Java e melhor que acertar sozinho: o mesmo algoritmo
     * precisa receber a mesma classe nas duas linguagens.
     */
    private static int somaDoCorpoDoSwitch(NoDeC escolha, String nome) {
        int total = 0;
        for (NoDeC filho : escolha.filhos()) {
            List<NoDeC> comandos = filho.tipo() == NoDeC.Tipo.BLOCO ? filho.filhos() : List.of(filho);
            for (NoDeC comando : comandos) {
                total += contarPorCaminho(comando, nome);
            }
        }
        return total;
    }

    private static int contarBloco(List<NoDeC> comandos, String nome) {
        int acumulado = 0;
        int melhorCaminhoQueSai = 0;
        for (NoDeC comando : comandos) {
            if (ehGuardaQueSai(comando)) {
                int custoDaGuarda = contarNoTexto(comando.condicao(), nome);
                melhorCaminhoQueSai = Math.max(melhorCaminhoQueSai,
                        acumulado + custoDaGuarda + contarPorCaminho(comando.filhos().get(0), nome));
                acumulado += custoDaGuarda;
                continue;
            }
            if (sempreSai(comando)) {
                return Math.max(melhorCaminhoQueSai, acumulado + contarPorCaminho(comando, nome));
            }
            acumulado += contarPorCaminho(comando, nome);
        }
        return Math.max(melhorCaminhoQueSai, acumulado);
    }

    private static int maiorDosFilhos(List<NoDeC> filhos, String nome) {
        return filhos.stream().mapToInt(filho -> contarPorCaminho(filho, nome)).max().orElse(0);
    }

    /** {@code if (n <= 1) return 1;} — guarda sem {@code else} cujo ramo sai da funcao. */
    private static boolean ehGuardaQueSai(NoDeC no) {
        return no.tipo() == NoDeC.Tipo.IF && no.filhos().size() == 1 && sempreSai(no.filhos().get(0));
    }

    private static boolean sempreSai(NoDeC no) {
        return switch (no.tipo()) {
            case COMANDO -> SAIDAS.contains(primeiraPalavra(no.condicao()));
            case BLOCO -> no.filhos().stream().anyMatch(RecursaoEmC::sempreSai);
            case IF -> no.filhos().size() == 2 && no.filhos().stream().allMatch(RecursaoEmC::sempreSai);
            default -> false;
        };
    }

    /**
     * Auto-chamadas no texto. O ternario e tratado a parte: {@code return f(a) > f(b) ? ... }
     * poe duas ocorrencias no texto, mas {@code cond ? x : y} executa so um dos lados, e contar os
     * dois transformaria uma recursao linear numa arvore binaria exponencial.
     */
    private static int contarNoTexto(String texto, String nome) {
        List<String> ternario = partesDoTernario(texto);
        if (ternario.size() == 3) {
            return contarOcorrencias(ternario.get(0), nome)
                    + Math.max(contarNoTexto(ternario.get(1), nome), contarNoTexto(ternario.get(2), nome));
        }
        return contarOcorrencias(texto, nome);
    }

    private static List<String> partesDoTernario(String texto) {
        int interrogacao = posicaoNoNivelZero(texto, '?', 0);
        if (interrogacao < 0) {
            return List.of(texto);
        }
        int doisPontos = posicaoNoNivelZero(texto, ':', interrogacao + 1);
        if (doisPontos < 0) {
            return List.of(texto);
        }
        return List.of(texto.substring(0, interrogacao),
                texto.substring(interrogacao + 1, doisPontos),
                texto.substring(doisPontos + 1));
    }

    private static int posicaoNoNivelZero(String texto, char alvo, int inicio) {
        int profundidade = 0;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '(' || c == '[') {
                profundidade++;
            } else if (c == ')' || c == ']') {
                profundidade--;
            } else if (c == alvo && profundidade == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int contarOcorrencias(String texto, String nome) {
        return (int) ExpressoesDeC.chamadasEm(texto).stream()
                .filter(chamada -> chamada.nome().equals(nome))
                .count();
    }

    // ---------------------------------------------------------------- b: como o argumento encolhe

    private static Recursao classificarReducao(FuncaoDeC funcao, int porCaminho) {
        if (temAutoChamadaEmLaco(funcao)) {
            return new Recursao(Reducao.BACKTRACKING, porCaminho, 2, false, 0,
                    "auto-chamada dentro de laco: assumida busca exaustiva");
        }

        Set<String> parametros = new LinkedHashSet<>(funcao.parametros());
        Set<String> pontosMedios = pontosMedios(funcao, parametros);
        Set<String> locais = locaisDe(funcao, parametros);
        Sinais sinais = lerSinais(funcao, parametros, pontosMedios, locais);

        if (sinais.divisiva) {
            return new Recursao(Reducao.DIVISIVA, porCaminho, sinais.fator, false, 0,
                    suposicaoDaDivisao(sinais));
        }
        if (sinais.subtrativa) {
            return new Recursao(Reducao.SUBTRATIVA, porCaminho, 1, false, 0, null);
        }
        if (sinais.estrutural) {
            return new Recursao(Reducao.ESTRUTURAL, porCaminho, 2, false, 0,
                    "recursao sobre uma estrutura de dados; assumido um passo por elemento");
        }
        if (sinais.derivadoDeLocal && porCaminho >= 2 && parametros.size() >= 2) {
            return new Recursao(Reducao.DIVISIVA, porCaminho, 2, false, 0,
                    "recursao sobre subintervalos delimitados por um pivo; assumida divisao balanceada");
        }
        return new Recursao(Reducao.INDETERMINADA, porCaminho, 2, false, 0, null);
    }

    /** Acumula o que os argumentos das auto-chamadas revelam sobre a reducao. */
    private static final class Sinais {
        private boolean divisiva;
        private boolean porResto;
        private boolean subtrativa;
        private boolean estrutural;
        private boolean derivadoDeLocal;
        private int fator = Integer.MAX_VALUE;

        private void divisivaCom(int divisor) {
            divisiva = true;
            fator = Math.min(fator, divisor);
        }
    }

    private static Sinais lerSinais(FuncaoDeC funcao, Set<String> parametros,
            Set<String> pontosMedios, Set<String> locais) {
        Sinais sinais = new Sinais();
        for (Chamada chamada : autoChamadas(funcao.corpo(), funcao.nome())) {
            for (String argumento : ParserEstruturalDeC.dividirNoNivelZero(chamada.argumentos(), ',')) {
                classificarArgumento(argumento, parametros, pontosMedios, locais, sinais);
            }
        }
        return sinais;
    }

    private static void classificarArgumento(String argumento, Set<String> parametros,
            Set<String> pontosMedios, Set<String> locais, Sinais sinais) {
        if (referencia(argumento, pontosMedios)) {
            sinais.divisivaCom(2);
            return;
        }
        int divisor = divisaoDeParametro(argumento, parametros);
        if (divisor > 0) {
            sinais.divisivaCom(divisor);
            return;
        }
        if (restoEntreParametros(argumento, parametros)) {
            sinais.divisivaCom(2);
            sinais.porResto = true;
            return;
        }
        if (subtracaoDeParametro(argumento, parametros)) {
            sinais.subtrativa = true;
            return;
        }
        if (ehAcessoEstrutural(argumento, parametros)) {
            sinais.estrutural = true;
            return;
        }
        if (referencia(argumento, locais)) {
            sinais.derivadoDeLocal = true;
        }
    }

    private static String suposicaoDaDivisao(Sinais sinais) {
        if (sinais.subtrativa) {
            return "reducao mista (n/b e n-c) nas auto-chamadas; assumida a divisiva, que domina";
        }
        if (sinais.porResto) {
            return "reducao por resto entre parametros, como no algoritmo de Euclides; "
                    + "assumida queda geometrica do argumento";
        }
        return null;
    }

    /** {@code f(n / 2)}, {@code f(n >> 1)}: devolve o divisor, ou 0 quando nao ha. */
    private static int divisaoDeParametro(String argumento, Set<String> parametros) {
        if (!referencia(argumento, parametros)) {
            return 0;
        }
        var achado = DIVISAO_POR_CONSTANTE.matcher(argumento);
        while (achado.find()) {
            int literal = Integer.parseInt(achado.group(2));
            if (achado.group(1).equals("/") && literal >= 2) {
                return literal;
            }
            if (achado.group(1).equals(">>") && literal >= 1) {
                return 1 << literal;
            }
        }
        return 0;
    }

    /**
     * {@code f(b, a % b)}: o argumento cai por resto. E a forma do algoritmo de Euclides, cujo
     * numero de passos e logaritmico (Lame). A regra e estreita de proposito — exige parametro dos
     * <b>dois</b> lados do {@code %}, porque {@code f(n % 2)} cai para uma constante num passo so.
     */
    private static boolean restoEntreParametros(String argumento, Set<String> parametros) {
        int resto = argumento.indexOf('%');
        if (resto < 0) {
            return false;
        }
        return referencia(argumento.substring(0, resto), parametros)
                && referencia(argumento.substring(resto + 1), parametros);
    }

    /** {@code f(n - 1)} ou {@code f(--n)}. */
    private static boolean subtracaoDeParametro(String argumento, Set<String> parametros) {
        if (!referencia(argumento, parametros)) {
            return false;
        }
        return Pattern.compile("-\\s*[0-9]+").matcher(argumento).find() || argumento.contains("--");
    }

    private static boolean ehAcessoEstrutural(String argumento, Set<String> parametros) {
        return ACESSO_ESTRUTURAL.matcher(argumento).find() && referencia(argumento, parametros);
    }

    private static boolean temAutoChamadaEmLaco(FuncaoDeC funcao) {
        return funcao.corpo().comDescendentes().stream()
                .filter(NoDeC::ehLaco)
                .anyMatch(laco -> !autoChamadas(laco, funcao.nome()).isEmpty());
    }

    private static List<Chamada> autoChamadas(NoDeC raiz, String nome) {
        List<Chamada> proprias = new ArrayList<>();
        for (Chamada chamada : ExpressoesDeC.chamadasEm(raiz.textoDaSubarvore())) {
            if (chamada.nome().equals(nome)) {
                proprias.add(chamada);
            }
        }
        return proprias;
    }

    /** Variaveis da funcao que nascem de uma divisao por constante sobre os parametros. */
    private static Set<String> pontosMedios(FuncaoDeC funcao, Set<String> parametros) {
        Set<String> medios = new LinkedHashSet<>();
        for (Atribuicao atribuicao : atribuicoesDe(funcao)) {
            if (DIVISAO_POR_CONSTANTE.matcher(atribuicao.valor()).find()
                    && referencia(atribuicao.valor(), parametros)) {
                medios.add(atribuicao.alvo());
            }
        }
        return medios;
    }

    private static Set<String> locaisDe(FuncaoDeC funcao, Set<String> parametros) {
        Set<String> locais = new LinkedHashSet<>();
        atribuicoesDe(funcao).forEach(atribuicao -> locais.add(atribuicao.alvo()));
        locais.removeAll(parametros);
        return locais;
    }

    private static List<Atribuicao> atribuicoesDe(FuncaoDeC funcao) {
        List<Atribuicao> atribuicoes = new ArrayList<>();
        funcao.corpo().comDescendentes()
                .forEach(no -> atribuicoes.addAll(ExpressoesDeC.atribuicoesEm(no.textoProprio())));
        return atribuicoes;
    }

    private static boolean referencia(String expressao, Set<String> nomes) {
        return ExpressoesDeC.variaveisEm(expressao).stream().anyMatch(nomes::contains);
    }

    private static String primeiraPalavra(String texto) {
        String limpo = texto.trim();
        int fim = 0;
        while (fim < limpo.length() && Character.isJavaIdentifierPart(limpo.charAt(fim))) {
            fim++;
        }
        return limpo.substring(0, fim);
    }
}
