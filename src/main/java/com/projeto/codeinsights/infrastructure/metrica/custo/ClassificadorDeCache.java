package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.projeto.codeinsights.infrastructure.metrica.custo.UsoDeCache.Forma;
import com.projeto.codeinsights.infrastructure.metrica.custo.UsoDeCache.Papel;

/**
 * Decide se um array, mapa ou conjunto visivel a um metodo recursivo <b>limita a recursao</b>.
 * <p>
 * Detecta o idioma {@code if (memo[n] != VAZIO) return memo[n]; ... memo[n] = ...}
 * (e o equivalente com {@code visitado[u]}): a arvore de recursao e podada, e o custo
 * passa a ser o numero de estados distintos - a dimensao do cache - vezes o trabalho
 * por estado. E o que separa Fibonacci ingenuo, {@code O(2^n)}, do memoizado, {@code O(n)}.
 */
final class ClassificadorDeCache {

    private static final Set<String> CACHES_ASSOCIATIVOS = Set.of(
            "Map", "HashMap", "TreeMap", "LinkedHashMap", "Set", "HashSet", "TreeSet", "LinkedHashSet");
    private static final Set<String> ESCRITAS_DE_CACHE = Set.of("put", "add");

    private ClassificadorDeCache() {
    }

    /**
     * O papel da estrutura que melhor limita a recursao, ou {@link UsoDeCache#NENHUM}: uma
     * memoizacao vence uma marca desfeita (e a que poda o tempo), e entre papeis iguais vence o
     * cache de maior dimensao.
     *
     * @param autoChamadas as chamadas do metodo a si mesmo; a guarda que retorna nao precisa delas,
     *                     mas a desmarcacao na volta e a protecao por envolvimento usam cada uma
     */
    static UsoDeCache classificar(MethodDeclaration metodo, Collection<MethodCallExpr> autoChamadas) {
        List<UsoDeCache> usos = candidatosACache(metodo).entrySet().stream()
                .map(candidato -> classificarCandidato(metodo, autoChamadas, candidato.getKey(), candidato.getValue()))
                .filter(uso -> uso.papel() != Papel.NENHUM)
                .toList();
        return melhorDe(usos, Papel.MEMOIZACAO)
                .or(() -> melhorDe(usos, Papel.MARCA_DESFEITA))
                .orElse(UsoDeCache.NENHUM);
    }

    private static Optional<UsoDeCache> melhorDe(List<UsoDeCache> usos, Papel papel) {
        return usos.stream()
                .filter(uso -> uso.papel() == papel)
                .max(Comparator.comparingInt(UsoDeCache::dimensoes));
    }

    /**
     * A consulta poda a recursao e ha escrita? Entao e memoizacao - a menos que a escrita seja
     * desfeita na volta, quando e apenas a marca do caminho de um backtracking.
     * As formas novas de protecao desfazem pela regra conservadora, a guarda que retorna pela estreita.
     */
    private static UsoDeCache classificarCandidato(MethodDeclaration metodo, Collection<MethodCallExpr> autoChamadas,
            String cache, int dimensoes) {
        Forma forma = temEscrita(metodo, cache) ? formaDeProtecao(metodo, autoChamadas, cache) : Forma.NENHUMA;
        if (forma == Forma.NENHUMA) {
            return UsoDeCache.NENHUM;
        }
        boolean formaNova = forma != Forma.GUARDA_QUE_RETORNA;
        Papel papel = DesmarcacaoNaVolta.desfazNaVolta(metodo, cache, autoChamadas, formaNova)
                ? Papel.MARCA_DESFEITA
                : Papel.MEMOIZACAO;
        return new UsoDeCache(papel, dimensoes, forma);
    }

    /** A guarda que retorna vale como esta; so sem ela se procura o {@code if} que envolve as chamadas. */
    private static Forma formaDeProtecao(MethodDeclaration metodo, Collection<MethodCallExpr> autoChamadas,
            String cache) {
        if (temGuardaDeLeitura(metodo, cache)) {
            return Forma.GUARDA_QUE_RETORNA;
        }
        return ProtecaoPorEnvolvimento.forma(metodo, cache, autoChamadas);
    }

    // ----- candidatos -----

    /** Arrays e mapas visiveis ao metodo e que sobrevivem entre chamadas: parametros e campos. */
    private static Map<String, Integer> candidatosACache(MethodDeclaration metodo) {
        Map<String, Integer> candidatos = new HashMap<>();
        metodo.getParameters().forEach(parametro -> dimensoesDe(parametro.getType())
                .ifPresent(dimensoes -> candidatos.put(parametro.getNameAsString(), dimensoes)));
        metodo.findAncestor(ClassOrInterfaceDeclaration.class).ifPresent(classe -> classe.getFields().stream()
                .flatMap(campo -> campo.getVariables().stream())
                .forEach(variavel -> dimensoesDe(variavel.getType())
                        .ifPresent(dimensoes -> candidatos.put(variavel.getNameAsString(), dimensoes))));
        return candidatos;
    }

    private static Optional<Integer> dimensoesDe(Type tipo) {
        if (tipo instanceof ArrayType arranjo) {
            return Optional.of(1 + dimensoesDe(arranjo.getComponentType()).orElse(0));
        }
        if (tipo instanceof ClassOrInterfaceType classe && CACHES_ASSOCIATIVOS.contains(classe.getNameAsString())) {
            return Optional.of(1);
        }
        return Optional.empty();
    }

    // ----- protecao: a guarda que retorna -----

    /**
     * A guarda precisa <b>consultar o cache pela chave</b> ({@code memo[n]}, {@code memo.get(n)}),
     * nao apenas mencionar a estrutura. Sem essa exigencia, {@code if (k == v.length) return;} num
     * backtracking que escreve {@code v[k]} seria lido como memoizacao, e uma busca exponencial
     * viraria O(n).
     */
    private static boolean temGuardaDeLeitura(MethodDeclaration metodo, String cache) {
        return metodo.findAll(IfStmt.class).stream()
                .filter(condicional -> AstUtils.sempreRetorna(condicional.getThenStmt()))
                .anyMatch(condicional -> AcessosAoCache.consulta(condicional.getCondition(), cache));
    }

    // ----- escrita -----

    private static boolean temEscrita(MethodDeclaration metodo, String cache) {
        boolean porAtribuicao = metodo.findAll(AssignExpr.class).stream()
                .anyMatch(atribuicao -> AcessosAoCache.escreveEm(atribuicao.getTarget(), cache));
        boolean porChamada = metodo.findAll(MethodCallExpr.class).stream()
                .filter(chamada -> ESCRITAS_DE_CACHE.contains(chamada.getNameAsString()))
                .anyMatch(chamada -> AcessosAoCache.temEscopo(chamada, cache));
        return porAtribuicao || porChamada;
    }
}
