package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;

/**
 * Grafo de chamadas entre os metodos da propria unidade de compilacao.
 * <p>
 * Serve para detectar <b>recursao mutua</b> ({@code par -> impar -> par}), que nao
 * aparece como auto-chamada em nenhum metodo isolado. Sem isso o analisador de espaco
 * concluiria "nenhuma recursao, pilha O(1)" com confianca alta - um erro silencioso.
 */
public final class GrafoDeChamadas {

    private GrafoDeChamadas() {
    }

    /** Existe um ciclo de comprimento >= 2 (recursao mutua)? Auto-chamadas nao contam. */
    public static boolean temCicloIndireto(CompilationUnit unidade) {
        Map<String, Set<String>> arestas = arestas(unidade);
        Set<String> encerrados = new HashSet<>();
        Set<String> naPilha = new HashSet<>();
        return arestas.keySet().stream().anyMatch(no -> alcancaCiclo(no, arestas, encerrados, naPilha));
    }

    private static boolean alcancaCiclo(String no, Map<String, Set<String>> arestas,
            Set<String> encerrados, Set<String> naPilha) {
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

    private static Map<String, Set<String>> arestas(CompilationUnit unidade) {
        Map<String, MethodDeclaration> declarados = new HashMap<>();
        unidade.findAll(MethodDeclaration.class)
                .forEach(metodo -> declarados.put(AstUtils.chaveDoMetodo(metodo), metodo));

        Map<String, Set<String>> arestas = new HashMap<>();
        declarados.forEach((chave, metodo) -> arestas.put(chave, metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> chamada.getScope().map(escopo -> escopo instanceof ThisExpr).orElse(true))
                .filter(chamada -> !AstUtils.ehAutoChamada(chamada, metodo))
                .map(chamada -> chamada.getNameAsString() + "/" + chamada.getArguments().size())
                .filter(declarados::containsKey)
                .collect(HashSet::new, Set::add, Set::addAll)));
        return arestas;
    }
}
