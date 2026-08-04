package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.projeto.codeinsights.infrastructure.metrica.c.ExpressoesDeC.Chamada;
import com.projeto.codeinsights.infrastructure.metrica.c.ProgramaDeC.FuncaoDeC;
import com.projeto.codeinsights.infrastructure.metrica.custo.Custo;
import com.projeto.codeinsights.infrastructure.metrica.custo.CustoAvaliado;
import com.projeto.codeinsights.infrastructure.metrica.custo.SolucionadorDeRecorrencia;

/**
 * Avalia o custo de tempo de um programa em C compondo custos sobre a arvore estrutural — a mesma
 * mecanica do {@code AvaliadorDeCusto} do lado Java, e por isso o mesmo algoritmo escrito nas duas
 * linguagens recebe a mesma classe:
 * <ul>
 *   <li>sequencia de comandos: {@code max} dos custos;</li>
 *   <li>laco: {@link IteracoesDeLacoEmC} x custo do corpo — <b>com a condicao dentro do produto</b>,
 *       que e o que faz {@code for (i = 0; i < strlen(s); i++)} sair como O(n^2);</li>
 *   <li>chamada a funcao do arquivo: o custo dela, memoizado;</li>
 *   <li>chamada a biblioteca: {@link BibliotecaDeC};</li>
 *   <li>funcao recursiva: {@link SolucionadorDeRecorrencia} sobre a recorrencia de
 *       {@link RecursaoEmC} — literalmente o mesmo Teorema Mestre do lado Java.</li>
 * </ul>
 * O ponto de entrada e {@code main}; sem ele, as raizes do grafo de chamadas. Codigo morto nao
 * contamina a estimativa.
 * <p>
 * <b>Uma diferenca real entre as linguagens, e nao uma convencao:</b> em Java {@code new int[n]}
 * zera n posicoes e custa O(n) de tempo. Em C, {@code malloc} entrega memoria sem tocar nela e
 * custa O(1); quem zera e {@code calloc}, que por isso esta na tabela como linear. Igualar as duas
 * seria cobrar de C um trabalho que o programa nao faz.
 */
final class CustoDeTempoEmC {

    private final Map<String, FuncaoDeC> funcoes = new HashMap<>();
    private final Map<String, CustoAvaliado> memo = new HashMap<>();
    private final Deque<String> emAvaliacao = new ArrayDeque<>();
    private final Set<String> constantes;

    private CustoDeTempoEmC(ProgramaDeC programa, Set<String> constantes) {
        this.constantes = constantes;
        programa.funcoes().forEach(funcao -> funcoes.put(funcao.chave(), funcao));
    }

    static CustoAvaliado doPrograma(ProgramaDeC programa, Set<String> constantes) {
        if (!programa.integro()) {
            return CustoAvaliado.desconhecido(programa.motivo());
        }
        CustoDeTempoEmC avaliador = new CustoDeTempoEmC(programa, constantes);
        return avaliador.pontosDeEntrada(programa).stream()
                .map(avaliador::custoDaFuncao)
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }

    private List<FuncaoDeC> pontosDeEntrada(ProgramaDeC programa) {
        Optional<FuncaoDeC> principal = programa.funcoes().stream()
                .filter(funcao -> funcao.nome().equals("main"))
                .findFirst();
        if (principal.isPresent()) {
            return List.of(principal.get());
        }
        Set<String> chamadas = nomesChamadosPorOutraFuncao(programa);
        List<FuncaoDeC> raizes = programa.funcoes().stream()
                .filter(funcao -> !chamadas.contains(funcao.nome()))
                .toList();
        return raizes.isEmpty() ? programa.funcoes() : raizes;
    }

    /** Auto-chamada nao tira uma funcao da raiz: uma funcao recursiva que ninguem chama e raiz. */
    private Set<String> nomesChamadosPorOutraFuncao(ProgramaDeC programa) {
        Set<String> chamados = new HashSet<>();
        for (FuncaoDeC funcao : programa.funcoes()) {
            ExpressoesDeC.chamadasEm(funcao.corpo().textoDaSubarvore()).stream()
                    .map(Chamada::nome)
                    .filter(nome -> !nome.equals(funcao.nome()))
                    .forEach(chamados::add);
        }
        return chamados;
    }

    private CustoAvaliado custoDaFuncao(FuncaoDeC funcao) {
        CustoAvaliado memorizado = memo.get(funcao.chave());
        if (memorizado != null) {
            return memorizado;
        }
        if (emAvaliacao.contains(funcao.chave())) {
            return CustoAvaliado.desconhecido(
                    "recursao mutua envolvendo `%s`: nao ha recorrencia de uma funcao so a resolver"
                            .formatted(funcao.nome()));
        }
        emAvaliacao.push(funcao.chave());
        try {
            CustoAvaliado corpo = custoDoNo(funcao.corpo(), funcao);
            CustoAvaliado resultado = RecursaoEmC.ehRecursiva(funcao)
                    ? SolucionadorDeRecorrencia.resolver(RecursaoEmC.analisar(funcao), corpo)
                    : corpo;
            memo.put(funcao.chave(), resultado);
            return resultado;
        } finally {
            emAvaliacao.pop();
        }
    }

    private CustoAvaliado custoDoNo(NoDeC no, FuncaoDeC atual) {
        return switch (no.tipo()) {
            case COMANDO -> custoDoTexto(no.condicao(), atual);
            // Rotulo nao executa nada: quem custa e o comando que vem depois dele.
            case ROTULO -> CustoAvaliado.exato(Custo.CONSTANTE);
            case BLOCO -> maiorDosFilhos(no, atual);
            case IF, SWITCH -> custoDoTexto(no.condicao(), atual).mais(maiorDosFilhos(no, atual));
            case FOR -> custoDoFor(no, atual);
            case WHILE, DO -> custoDeLaco(no, custoDoTexto(no.condicao(), atual).mais(maiorDosFilhos(no, atual)));
        };
    }

    private CustoAvaliado custoDoFor(NoDeC laco, FuncaoDeC atual) {
        CustoAvaliado porVolta = custoDoTexto(laco.condicao(), atual)
                .mais(custoDoTexto(laco.passo(), atual))
                .mais(maiorDosFilhos(laco, atual));
        return custoDoTexto(laco.inicio(), atual).mais(custoDeLaco(laco, porVolta));
    }

    private CustoAvaliado custoDeLaco(NoDeC laco, CustoAvaliado porVolta) {
        return IteracoesDeLacoEmC.de(laco, constantes).vezes(porVolta);
    }

    private CustoAvaliado maiorDosFilhos(NoDeC no, FuncaoDeC atual) {
        return no.filhos().stream()
                .map(filho -> custoDoNo(filho, atual))
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }

    private CustoAvaliado custoDoTexto(String texto, FuncaoDeC atual) {
        return ExpressoesDeC.chamadasEm(texto).stream()
                .map(chamada -> custoDaChamada(chamada, atual))
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }

    private CustoAvaliado custoDaChamada(Chamada chamada, FuncaoDeC atual) {
        if (atual != null && chamada.nome().equals(atual.nome())) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        FuncaoDeC alvo = funcoes.get(chamada.nome() + "/" + aridade(chamada));
        if (alvo == null) {
            alvo = funcoes.values().stream()
                    .filter(funcao -> funcao.nome().equals(chamada.nome()))
                    .findFirst()
                    .orElse(null);
        }
        if (alvo != null) {
            return custoDaFuncao(alvo);
        }
        return BibliotecaDeC.custo(chamada.nome())
                .orElseGet(() -> CustoAvaliado.estimado(Custo.CONSTANTE,
                        "chamada externa `%s` desconhecida; assumida O(1)".formatted(chamada.nome())));
    }

    private static int aridade(Chamada chamada) {
        return chamada.argumentos().isBlank()
                ? 0
                : ParserEstruturalDeC.dividirNoNivelZero(chamada.argumentos(), ',').size();
    }
}
