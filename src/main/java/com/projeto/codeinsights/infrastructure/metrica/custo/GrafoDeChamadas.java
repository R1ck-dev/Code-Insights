package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

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

    /** Existe um ciclo de comprimento >= 2 (recursao mutua)? Auto-chamadas nao contam. */
    public static boolean temCicloIndireto(ResolvedorDeChamadas resolvedor) {
        Map<MethodDeclaration, Set<MethodDeclaration>> arestas = new IdentityHashMap<>();
        resolvedor.metodos().forEach(metodo -> arestas.put(metodo, resolvedor.chamadosPor(metodo)));
        Set<MethodDeclaration> encerrados = ResolvedorDeChamadas.conjuntoPorIdentidade();
        Set<MethodDeclaration> naPilha = ResolvedorDeChamadas.conjuntoPorIdentidade();
        return arestas.keySet().stream().anyMatch(no -> alcancaCiclo(no, arestas, encerrados, naPilha));
    }

    private static boolean alcancaCiclo(MethodDeclaration no, Map<MethodDeclaration, Set<MethodDeclaration>> arestas,
            Set<MethodDeclaration> encerrados, Set<MethodDeclaration> naPilha) {
        if (naPilha.contains(no)) {
            return true;
        }
        if (encerrados.contains(no)) {
            return false;
        }
        naPilha.add(no);
        boolean achou = arestas.getOrDefault(no, Set.of()).stream()
                .anyMatch(vizinho -> alcancaCiclo(vizinho, arestas, encerrados, naPilha));
        naPilha.remove(no);
        encerrados.add(no);
        return achou;
    }
}
