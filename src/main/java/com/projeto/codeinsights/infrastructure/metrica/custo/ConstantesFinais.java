package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;

/**
 * Quais nomes valem uma constante: {@code static final int MAX = 100} (ou {@code final} local) usado como
 * limite de laco ou tamanho de vetor nao depende da entrada, como o literal que ele nomeia.
 * <p>
 * Um nome e <b>constante final</b> quando e campo {@code final} (de instancia ou estatico) de tipo inteiro,
 * {@code long}, {@code char}, {@code short} ou {@code byte} com <b>inicializador literal</b> (ou
 * {@code -literal}), ou variavel local {@code final} nas mesmas condicoes. Ficam de fora, de proposito:
 * campo {@code final} sem inicializador (atribuido no construtor ou no bloco estatico), campo <b>nao</b>
 * {@code final} (nao se rastreia quem escreve nele) e expressao composta ({@code MAX - 1}).
 * <p>
 * A tabela de nomes e plana, entao o risco e a sombra: dentro de um metodo, parametro ou local que nao seja
 * constante final com o mesmo nome do campo tira o nome do conjunto <i>naquele metodo</i>. Do mesmo modo, um
 * nome declarado como campo constante numa classe e como campo qualquer noutra nao e constante.
 * <p>
 * O objeto e construido uma vez por unidade de compilacao e passado a quem pergunta; o conjunto de cada
 * metodo e calculado na primeira consulta e guardado nesta instancia (nao ha estado estatico).
 */
final class ConstantesFinais {

    private final Set<String> campos;
    private final Map<Node, Set<String>> porEscopo = new IdentityHashMap<>();

    private ConstantesFinais(Set<String> campos) {
        this.campos = campos;
    }

    static ConstantesFinais de(CompilationUnit unidade) {
        return new ConstantesFinais(camposConstantes(unidade));
    }

    /** Literal ou nome de constante final visivel no ponto onde a expressao esta. */
    boolean ehConstante(Expression expressao) {
        if (ehLiteral(expressao)) {
            return true;
        }
        return expressao instanceof NameExpr nome && visiveisEm(expressao).contains(nome.getNameAsString());
    }

    /** Literal inteiro, {@code long}, {@code char} ou {@code -literal} numerico. */
    static boolean ehLiteral(Expression expressao) {
        if (expressao.isIntegerLiteralExpr() || expressao.isLongLiteralExpr() || expressao.isCharLiteralExpr()) {
            return true;
        }
        return expressao instanceof UnaryExpr unario
                && unario.getOperator() == UnaryExpr.Operator.MINUS
                && (unario.getExpression().isIntegerLiteralExpr() || unario.getExpression().isLongLiteralExpr());
    }

    private Set<String> visiveisEm(Node no) {
        return escopoDe(no)
                .map(escopo -> porEscopo.computeIfAbsent(escopo, this::constantesDoEscopo))
                .orElse(campos);
    }

    /** O metodo, construtor ou bloco de inicializacao que contem o no; fora de todos, so valem os campos. */
    private static Optional<Node> escopoDe(Node no) {
        Optional<Node> pai = no.getParentNode();
        while (pai.isPresent()) {
            if (pai.get() instanceof CallableDeclaration<?> || pai.get() instanceof InitializerDeclaration) {
                return pai;
            }
            pai = pai.get().getParentNode();
        }
        return Optional.empty();
    }

    /** Campos constantes e locais finais literais, menos tudo o que o escopo redeclara como variavel. */
    private Set<String> constantesDoEscopo(Node escopo) {
        Set<String> visiveis = new HashSet<>(campos);
        Set<String> sombreados = new HashSet<>();
        for (VariableDeclarator declaracao : escopo.findAll(VariableDeclarator.class)) {
            if (ehLocalConstante(declaracao)) {
                visiveis.add(declaracao.getNameAsString());
            } else {
                sombreados.add(declaracao.getNameAsString());
            }
        }
        escopo.findAll(Parameter.class).forEach(parametro -> sombreados.add(parametro.getNameAsString()));
        visiveis.removeAll(sombreados);
        return visiveis;
    }

    private static boolean ehLocalConstante(VariableDeclarator declaracao) {
        return declaracao.getParentNode().orElse(null) instanceof VariableDeclarationExpr local
                && local.isFinal()
                && temTipoInteiro(declaracao.getType())
                && temInicializadorLiteral(declaracao);
    }

    /** Nome declarado como campo constante em toda parte onde e declarado como campo. */
    private static Set<String> camposConstantes(CompilationUnit unidade) {
        Set<String> constantes = new HashSet<>();
        Set<String> variaveis = new HashSet<>();
        for (FieldDeclaration campo : unidade.findAll(FieldDeclaration.class)) {
            for (VariableDeclarator declaracao : campo.getVariables()) {
                if (ehCampoConstante(campo, declaracao)) {
                    constantes.add(declaracao.getNameAsString());
                } else {
                    variaveis.add(declaracao.getNameAsString());
                }
            }
        }
        constantes.removeAll(variaveis);
        return constantes;
    }

    private static boolean ehCampoConstante(FieldDeclaration campo, VariableDeclarator declaracao) {
        return (campo.isFinal() || ehMembroDeInterface(campo))
                && temTipoInteiro(declaracao.getType())
                && temInicializadorLiteral(declaracao);
    }

    /** Campo de interface e implicitamente {@code static final}. */
    private static boolean ehMembroDeInterface(FieldDeclaration campo) {
        return campo.getParentNode().orElse(null) instanceof ClassOrInterfaceDeclaration tipo && tipo.isInterface();
    }

    private static boolean temInicializadorLiteral(VariableDeclarator declaracao) {
        return declaracao.getInitializer().map(ConstantesFinais::ehLiteral).orElse(false);
    }

    private static boolean temTipoInteiro(Type tipo) {
        if (tipo instanceof VarType) {
            return true;
        }
        return tipo instanceof PrimitiveType primitivo && switch (primitivo.getType()) {
            case INT, LONG, CHAR, SHORT, BYTE -> true;
            default -> false;
        };
    }
}
