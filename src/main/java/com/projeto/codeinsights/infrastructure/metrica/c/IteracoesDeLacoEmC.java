package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.projeto.codeinsights.infrastructure.metrica.c.ExpressoesDeC.Atribuicao;
import com.projeto.codeinsights.infrastructure.metrica.custo.Custo;
import com.projeto.codeinsights.infrastructure.metrica.custo.CustoAvaliado;

/**
 * Quantas vezes um laco de C executa, em funcao do tamanho da entrada.
 * <p>
 * As regras sao <b>as mesmas</b> do {@code IteracoesDeLaco} do lado Java, e isso e um requisito, e
 * nao uma economia: se cada linguagem reconhecesse laco logaritmico por criterio proprio, a mesma
 * busca binaria receberia classes diferentes conforme a linguagem, e o eixo de complexidade da
 * pesquisa deixaria de comparar alunos entre si. Por isso o sinal de laco logaritmico continua
 * sendo "o intervalo que governa a parada e dividido a cada volta", nas duas formas — indice
 * multiplicativo e intervalo que encolhe.
 * <p>
 * <b>Uma regra a mais que o lado Java, por necessidade de C.</b> Constante de {@code #define} conta
 * como limite literal: {@code for (i = 0; i < MAX; i++)} nao depende da entrada. Sem isso, o jeito
 * padrao de dimensionar vetor em C viraria O(n) em toda solucao do piloto. O lado Java tem a mesma
 * lacuna com {@code static final int} e ainda nao a trata — anotado como divida, porque a diferenca
 * so aparece em codigo que usa constante nomeada como limite de laco.
 */
final class IteracoesDeLacoEmC {

    /** {@code x / 2} ou {@code x >> 1}: divisao por constante, o sinal de intervalo que encolhe. */
    private static final Pattern DIVISAO_POR_CONSTANTE = Pattern.compile("(/|>>)\\s*[0-9]+");

    private static final Pattern COMPARACAO = Pattern.compile("<=|>=|==|!=|<|>");

    private static final Set<String> OPERADORES_MULTIPLICATIVOS = Set.of("*", "/", "<<", ">>");

    private IteracoesDeLacoEmC() {
    }

    static CustoAvaliado de(NoDeC laco, Set<String> constantes) {
        Set<String> terminacao = variaveisDaCondicao(laco, constantes);
        if (terminacao.isEmpty()) {
            return CustoAvaliado.estimado(Custo.LINEAR, "laco sem condicao de parada; assumidas n iteracoes");
        }
        if (ehLimitadoPorConstante(laco, constantes)) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        if (ehLogaritmico(laco, terminacao)) {
            return CustoAvaliado.exato(Custo.LOGARITMICO);
        }
        if (temPassoUnitario(laco, terminacao)) {
            return CustoAvaliado.exato(Custo.LINEAR);
        }
        return CustoAvaliado.estimado(Custo.LINEAR, "progressao do laco nao reconhecida; assumidas n iteracoes");
    }

    private static Set<String> variaveisDaCondicao(NoDeC laco, Set<String> constantes) {
        Set<String> variaveis = new LinkedHashSet<>(ExpressoesDeC.variaveisEm(laco.condicao()));
        variaveis.removeAll(constantes);
        return variaveis;
    }

    /** {@code for (i = 0; i < 26; i++)} e {@code for (i = 0; i < MAX; i++)} nao dependem da entrada. */
    private static boolean ehLimitadoPorConstante(NoDeC laco, Set<String> constantes) {
        if (laco.tipo() != NoDeC.Tipo.FOR || laco.inicio().isBlank()) {
            return false;
        }
        List<Atribuicao> inicializacoes = ExpressoesDeC.atribuicoesEm(laco.inicio());
        boolean iniciosLiterais = !inicializacoes.isEmpty()
                && inicializacoes.stream().allMatch(a -> ExpressoesDeC.ehLiteralInteiro(a.valor()));
        return iniciosLiterais && comparaComConstante(laco.condicao(), constantes);
    }

    /**
     * Uma <b>unica</b> comparacao contra constante. Condicao composta ({@code i < n && achou == 0})
     * nao conta: o lado Java olha o operador de topo da condicao, e num {@code &&} nenhum dos lados
     * e literal. Varrer a condicao inteira atras de qualquer comparacao com literal fazia o
     * {@code achou == 0} declarar o laco constante — e um laco de n voltas saia como O(1), que e a
     * direcao perigosa de errar.
     */
    private static boolean comparaComConstante(String condicao, Set<String> constantes) {
        if (temOperadorLogicoNoTopo(condicao)) {
            return false;
        }
        var achado = COMPARACAO.matcher(condicao);
        while (achado.find()) {
            String esquerda = condicao.substring(0, achado.start());
            String direita = condicao.substring(achado.end());
            if (ladoConstante(esquerda, constantes) || ladoConstante(direita, constantes)) {
                return true;
            }
        }
        return false;
    }

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

    /**
     * Lado sem nenhuma variavel livre <b>e</b> sem chamada. A chamada precisa entrar na conta:
     * {@code i < tamanho()} nao tem identificador solto — {@code tamanho} e consumido como nome de
     * funcao — e passaria por limite constante, quando o motor nao tem como provar que o retorno
     * nao cresce com a entrada.
     */
    private static boolean ladoConstante(String lado, Set<String> constantes) {
        String limpo = lado.trim();
        return !limpo.isEmpty()
                && !ExpressoesDeC.temChamada(limpo)
                && ExpressoesDeC.ehConstante(limpo, constantes);
    }

    private static boolean ehLogaritmico(NoDeC laco, Set<String> terminacao) {
        return temIndiceMultiplicativo(laco, terminacao) || temIntervaloQueEncolhe(laco, terminacao);
    }

    /** {@code i *= 2}, {@code i /= 2}, {@code i >>= 1} ou {@code i = i * 2} sobre a variavel da parada. */
    private static boolean temIndiceMultiplicativo(NoDeC laco, Set<String> terminacao) {
        return atribuicoesDoNivel(laco).stream()
                .filter(atribuicao -> terminacao.contains(atribuicao.alvo()))
                .anyMatch(IteracoesDeLacoEmC::ehAtualizacaoMultiplicativa);
    }

    private static boolean ehAtualizacaoMultiplicativa(Atribuicao atribuicao) {
        if (OPERADORES_MULTIPLICATIVOS.contains(atribuicao.operador())) {
            return true;
        }
        if (!atribuicao.operador().isEmpty()) {
            return false;
        }
        return multiplicaNoTopo(atribuicao.valor())
                && ExpressoesDeC.variaveisEm(atribuicao.valor()).contains(atribuicao.alvo());
    }

    /**
     * O operador que <b>governa</b> a expressao e multiplicativo. Procurar {@code *} ou {@code /}
     * em qualquer lugar do texto era um teste de substring: {@code resto = resto - moeda * qtd}
     * contem um {@code *}, e o laco passava a ser lido como logaritmico quando cada volta so tira
     * uma parcela — O(log n) no lugar de O(n), a direcao perigosa.
     * <p>
     * Como {@code +} e {@code -} tem precedencia menor, a presenca de qualquer um deles no nivel
     * zero significa que quem manda e a soma. E o mesmo criterio do lado Java, que exige uma
     * {@code BinaryExpr} multiplicativa na raiz do valor.
     */
    private static boolean multiplicaNoTopo(String valor) {
        int profundidade = 0;
        boolean multiplicativo = false;
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '(' || c == '[') {
                profundidade++;
            } else if (c == ')' || c == ']') {
                profundidade--;
            } else if (profundidade == 0 && (c == '+' || c == '-') && ehBinario(valor, i)) {
                return false;
            } else if (profundidade == 0 && (c == '*' || c == '/' || c == '<' || c == '>')) {
                multiplicativo = true;
            }
        }
        return multiplicativo;
    }

    /** {@code a - b} e binario; o {@code -} de {@code -1} e sinal e nao desmancha o produto. */
    private static boolean ehBinario(String valor, int i) {
        int anterior = i - 1;
        while (anterior >= 0 && Character.isWhitespace(valor.charAt(anterior))) {
            anterior--;
        }
        if (anterior < 0) {
            return false;
        }
        char c = valor.charAt(anterior);
        return Character.isJavaIdentifierPart(c) || c == ')' || c == ']';
    }

    /**
     * Busca binaria e afins: {@code meio} nasce de uma divisao sobre as variaveis da condicao, e
     * alguma delas passa a valer algo derivado de {@code meio}.
     */
    private static boolean temIntervaloQueEncolhe(NoDeC laco, Set<String> terminacao) {
        Set<String> pontosMedios = pontosMedios(laco, terminacao);
        if (pontosMedios.isEmpty()) {
            return false;
        }
        return atribuicoesDoNivel(laco).stream()
                .filter(atribuicao -> terminacao.contains(atribuicao.alvo()))
                .anyMatch(atribuicao -> referenciaAlguma(atribuicao.valor(), pontosMedios));
    }

    private static Set<String> pontosMedios(NoDeC laco, Set<String> terminacao) {
        Set<String> medios = new LinkedHashSet<>();
        for (Atribuicao atribuicao : atribuicoesDoNivel(laco)) {
            if (ehDivisaoSobre(atribuicao.valor(), terminacao)) {
                medios.add(atribuicao.alvo());
            }
        }
        return medios;
    }

    private static boolean ehDivisaoSobre(String valor, Set<String> terminacao) {
        return DIVISAO_POR_CONSTANTE.matcher(valor).find() && referenciaAlguma(valor, terminacao);
    }

    private static boolean referenciaAlguma(String expressao, Set<String> nomes) {
        return ExpressoesDeC.variaveisEm(expressao).stream().anyMatch(nomes::contains);
    }

    /** {@code i++}, {@code i--}, {@code i += k}: n iteracoes, com certeza. */
    private static boolean temPassoUnitario(NoDeC laco, Set<String> terminacao) {
        return atribuicoesDoNivel(laco).stream()
                .filter(atribuicao -> terminacao.contains(atribuicao.alvo()))
                .anyMatch(atribuicao -> atribuicao.operador().equals("+") || atribuicao.operador().equals("-"));
    }

    /**
     * Atribuicoes cujo laco envolvente imediato e este — as de um laco interno pertencem a ele, e
     * nao a este. E o equivalente do {@code AstUtils.lacoMaisProximo} do lado Java: sem esse
     * recorte, o {@code j *= 2} de um laco de dentro faria o laco de fora parecer logaritmico.
     */
    private static List<Atribuicao> atribuicoesDoNivel(NoDeC laco) {
        List<String> textos = new ArrayList<>();
        textos.add(laco.passo());
        laco.filhos().forEach(filho -> coletarTextos(filho, textos));

        List<Atribuicao> atribuicoes = new ArrayList<>();
        textos.forEach(texto -> atribuicoes.addAll(ExpressoesDeC.atribuicoesEm(texto)));
        return atribuicoes;
    }

    private static void coletarTextos(NoDeC no, List<String> textos) {
        if (no.ehLaco()) {
            return;
        }
        textos.add(no.textoProprio());
        no.filhos().forEach(filho -> coletarTextos(filho, textos));
    }
}
