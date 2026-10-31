package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.projeto.codeinsights.infrastructure.metrica.c.ExpressoesDeC.Chamada;
import com.projeto.codeinsights.infrastructure.metrica.c.ProgramaDeC.FuncaoDeC;
import com.projeto.codeinsights.infrastructure.metrica.custo.ComponentesFortementeConexos;

/**
 * Grafo de chamadas entre as funcoes definidas no proprio arquivo C — o par do
 * {@code GrafoDeChamadas} do lado Java, sobre o mesmo nucleo de componentes fortemente conexos.
 * <p>
 * Serve para detectar <b>recursao mutua</b> ({@code par -> impar -> par}), que nao aparece como
 * auto-chamada em nenhuma funcao isolada. Os nos sao os indices de {@link ProgramaDeC#funcoes()};
 * a auto-chamada fica fora das arestas (e a recursao simples, tratada por {@link RecursaoEmC}).
 */
final class GrafoDeChamadasEmC {

    private GrafoDeChamadasEmC() {
    }

    /**
     * A funcao do arquivo que a chamada invoca, ou vazio se ela vai para a biblioteca. Procura por
     * nome + aridade e, se a aridade nao casa, so pelo nome: em C nao ha sobrecarga, e contar argumentos
     * por virgulas erra quando um argumento e uma expressao com virgula dentro de parenteses.
     */
    static Optional<FuncaoDeC> funcaoChamada(ProgramaDeC programa, Chamada chamada) {
        return programa.porChave(chamada.nome() + "/" + aridade(chamada))
                .or(() -> programa.porNome(chamada.nome()));
    }

    private static int aridade(Chamada chamada) {
        return chamada.argumentos().isBlank()
                ? 0
                : ParserEstruturalDeC.dividirNoNivelZero(chamada.argumentos(), ',').size();
    }

    /** Os grupos de funcoes em recursao mutua (dois ou mais), como indices de {@code programa.funcoes()}. */
    static List<int[]> ciclos(ProgramaDeC programa) {
        List<FuncaoDeC> funcoes = programa.funcoes();
        Map<FuncaoDeC, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < funcoes.size(); i++) {
            indices.put(funcoes.get(i), i);
        }
        return ComponentesFortementeConexos.ciclicos(funcoes.size(),
                no -> vizinhosDe(programa, funcoes.get(no), indices));
    }

    /** Existe recursao mutua entre funcoes do arquivo? Auto-chamadas nao contam. */
    static boolean temCicloIndireto(ProgramaDeC programa) {
        return !ciclos(programa).isEmpty();
    }

    private static int[] vizinhosDe(ProgramaDeC programa, FuncaoDeC funcao, Map<FuncaoDeC, Integer> indices) {
        return ExpressoesDeC.chamadasEm(funcao.corpo().textoDaSubarvore()).stream()
                .map(chamada -> funcaoChamada(programa, chamada))
                .flatMap(Optional::stream)
                .filter(chamada -> chamada != funcao)
                .map(indices::get)
                .distinct()
                .mapToInt(Integer::intValue)
                .toArray();
    }
}
