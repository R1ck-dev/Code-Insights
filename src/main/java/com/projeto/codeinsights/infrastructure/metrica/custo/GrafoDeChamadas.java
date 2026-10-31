package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.github.javaparser.ast.body.MethodDeclaration;

/**
 * Grafo de chamadas entre os metodos da propria unidade de compilacao.
 * <p>
 * Serve para detectar <b>recursao mutua</b> ({@code par -> impar -> par}), que nao
 * aparece como auto-chamada em nenhum metodo isolado. Sem isso o analisador de espaco
 * concluiria "nenhuma recursao, pilha O(1)" com confianca alta - um erro silencioso.
 * <p>
 * As arestas vem do {@link ResolvedorDeChamadas}, entao {@code a.f()} chamando {@code b.g()} que
 * chama {@code a.f()} tambem fecha um ciclo.
 */
public final class GrafoDeChamadas {

    private GrafoDeChamadas() {
    }

    /**
     * Existe um ciclo de comprimento >= 2 (recursao mutua)? Auto-chamadas nao contam: o
     * {@link ResolvedorDeChamadas#chamadosPor} ja as deixa fora das arestas.
     */
    public static boolean temCicloIndireto(ResolvedorDeChamadas resolvedor) {
        return !ciclosPor(resolvedor, resolvedor::chamadosPor).isEmpty();
    }

    /**
     * Cada metodo que participa de uma recursao mutua <b>do tempo</b>, ligado a todos os membros do
     * seu ciclo (ele incluso). Metodos fora de ciclo — e a recursao simples — nao entram no mapa.
     * <p>
     * Aresta por receptor de nome ambiguo ({@link ResolvedorDeChamadas#chamadosPorSemReceptorAmbiguo})
     * nao conta: um ciclo fabricado pela tabela plana de tipos viraria um numero errado, e um ciclo
     * real perdido por isso cai em {@code ?}, que e honesto. {@link #temCicloIndireto} (espaco) nao
     * aplica esse filtro.
     */
    public static Map<MethodDeclaration, List<MethodDeclaration>> ciclos(ResolvedorDeChamadas resolvedor) {
        return ciclosPor(resolvedor, resolvedor::chamadosPorSemReceptorAmbiguo);
    }

    private static Map<MethodDeclaration, List<MethodDeclaration>> ciclosPor(ResolvedorDeChamadas resolvedor,
            Function<MethodDeclaration, Set<MethodDeclaration>> arestas) {
        List<MethodDeclaration> metodos = resolvedor.metodos();
        Map<MethodDeclaration, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < metodos.size(); i++) {
            indices.put(metodos.get(i), i);
        }
        Map<MethodDeclaration, List<MethodDeclaration>> cicloDoMetodo = new IdentityHashMap<>();
        for (int[] componente : ComponentesFortementeConexos.ciclicos(metodos.size(),
                no -> vizinhosDe(arestas.apply(metodos.get(no)), indices))) {
            List<MethodDeclaration> ciclo = Arrays.stream(componente).mapToObj(metodos::get).toList();
            ciclo.forEach(membro -> cicloDoMetodo.put(membro, ciclo));
        }
        return cicloDoMetodo;
    }

    private static int[] vizinhosDe(Set<MethodDeclaration> chamados, Map<MethodDeclaration, Integer> indices) {
        return chamados.stream()
                .map(indices::get)
                .filter(indice -> indice != null)
                .mapToInt(Integer::intValue)
                .toArray();
    }
}
