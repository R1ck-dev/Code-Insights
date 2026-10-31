package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
        List<MethodDeclaration> metodos = resolvedor.metodos();
        Map<MethodDeclaration, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < metodos.size(); i++) {
            indices.put(metodos.get(i), i);
        }
        return !ComponentesFortementeConexos.ciclicos(metodos.size(),
                no -> vizinhosDe(resolvedor, metodos.get(no), indices)).isEmpty();
    }

    private static int[] vizinhosDe(ResolvedorDeChamadas resolvedor, MethodDeclaration metodo,
            Map<MethodDeclaration, Integer> indices) {
        return resolvedor.chamadosPor(metodo).stream()
                .map(indices::get)
                .filter(indice -> indice != null)
                .mapToInt(Integer::intValue)
                .toArray();
    }
}
