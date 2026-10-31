package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;

/**
 * Nomes de campo/variavel/parametro declarados com <b>mais de um tipo distinto</b> na unidade.
 * <p>
 * {@link TiposDeVariavel} e uma tabela plana por nome e guarda um tipo so por nome (o ultimo
 * declarado), entao um receptor {@code outro.m()} cujo nome aparece em duas classes com tipos
 * diferentes resolve para o tipo errado, em silencio. Dois tipos contam como distintos pelo
 * <b>tipo declarado inteiro</b>, com argumentos genericos. Este conjunto diz <i>quais</i> nomes sofrem
 * disso, para que quem monta o grafo de chamadas do tempo possa nao confiar nessas arestas. Nao
 * resolve o homonimo (tipos por escopo e outra frente): so o aponta.
 */
final class NomesAmbiguos {

    private final Set<String> nomes;

    private NomesAmbiguos(Set<String> nomes) {
        this.nomes = nomes;
    }

    static NomesAmbiguos de(CompilationUnit unidade) {
        Map<String, Set<String>> tiposPorNome = new HashMap<>();
        unidade.findAll(Parameter.class)
                .forEach(parametro -> registrar(tiposPorNome, parametro.getNameAsString(), parametro.getType(), null));
        unidade.findAll(VariableDeclarator.class)
                .forEach(variavel -> registrar(tiposPorNome, variavel.getNameAsString(), variavel.getType(),
                        variavel.getInitializer().orElse(null)));
        return new NomesAmbiguos(tiposPorNome.entrySet().stream()
                .filter(entrada -> entrada.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()));
    }

    /**
     * O tipo do receptor depende de um nome ambiguo? Segue a cadeia ({@code a.b().c}) e e
     * conservador: basta um elo ambiguo. Conversao explicita e criacao ({@code new X()}) trazem o tipo
     * consigo e nao dependem de nome.
     */
    boolean receptorAmbiguo(Expression receptor) {
        if (receptor instanceof NameExpr nome) {
            return nomes.contains(nome.getNameAsString());
        }
        if (receptor instanceof FieldAccessExpr campo) {
            return nomes.contains(campo.getNameAsString());
        }
        if (receptor instanceof EnclosedExpr entreParenteses) {
            return receptorAmbiguo(entreParenteses.getInner());
        }
        if (receptor instanceof ArrayAccessExpr acesso) {
            return receptorAmbiguo(acesso.getName());
        }
        if (receptor instanceof MethodCallExpr chamada) {
            return chamada.getScope().filter(this::receptorAmbiguo).isPresent();
        }
        return false;
    }

    private static void registrar(Map<String, Set<String>> tiposPorNome, String nome, Type declarado,
            Expression inicializador) {
        tipoEfetivo(declarado, inicializador)
                .ifPresent(tipo -> tiposPorNome.computeIfAbsent(nome, chave -> new HashSet<>()).add(tipo));
    }

    /**
     * O tipo que {@link TiposDeVariavel} guarda ({@code var x = new X()} e {@code X}), mas com os
     * <b>argumentos genericos</b>: o resolvedor segue {@code lista.get(i).g()} pelo tipo declarado
     * inteiro, entao {@code List<X>} e {@code List<Y>} sao tipos distintos aqui, nao o mesmo
     * {@code List}. {@code A} e {@code A[]} tambem seguem distintos.
     */
    private static Optional<String> tipoEfetivo(Type declarado, Expression inicializador) {
        Type tipo = declarado instanceof VarType && inicializador instanceof ObjectCreationExpr criacao
                ? criacao.getType()
                : declarado;
        return TiposDeVariavel.nomeDe(tipo).map(simples -> tipo.asString().replaceAll("\\s+", ""));
    }
}
