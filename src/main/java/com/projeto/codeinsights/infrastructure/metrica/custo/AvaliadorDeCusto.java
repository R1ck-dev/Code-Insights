package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

/**
 * Avalia o custo de tempo de uma unidade de compilacao percorrendo a AST de baixo
 * para cima e <b>compondo</b> custos, em vez de casar padroes num AST achatado.
 * <p>
 * O motor anterior procurava a cadeia de lacos aninhados mais profunda da unidade
 * inteira, ignorando fronteiras de metodo. Isso subestimava um {@code main} que chama
 * um metodo O(n) dentro de um laco (o custo nao se multiplicava) e superestimava uma
 * solucao com um auxiliar caro que nem era chamado. Aqui ha um grafo de chamadas:
 * <ul>
 *   <li>sequencia de comandos: {@code max} dos custos (soma assintotica);</li>
 *   <li>laco: {@link IteracoesDeLaco} x custo do corpo;</li>
 *   <li>chamada a metodo local: o custo do metodo chamado, memoizado;</li>
 *   <li>chamada a biblioteca: {@link TabelaDeCustoApi};</li>
 *   <li>metodo recursivo: {@link SolucionadorDeRecorrencia} sobre a recorrencia extraida.</li>
 * </ul>
 * O ponto de entrada e {@code main}, se existir; senao, as raizes do grafo de chamadas
 * (metodos que ninguem chama). Assim, codigo morto nao contamina a estimativa.
 */
public final class AvaliadorDeCusto {

    private final TiposDeVariavel tipos;
    private final Map<String, MethodDeclaration> metodos = new HashMap<>();
    private final Map<String, CustoAvaliado> memo = new HashMap<>();
    private final Deque<String> emAvaliacao = new ArrayDeque<>();

    private AvaliadorDeCusto(CompilationUnit unidade) {
        this.tipos = TiposDeVariavel.de(unidade);
        unidade.findAll(MethodDeclaration.class)
                .forEach(metodo -> metodos.put(AstUtils.chaveDoMetodo(metodo), metodo));
    }

    public static CustoAvaliado doPrograma(CompilationUnit unidade) {
        AvaliadorDeCusto avaliador = new AvaliadorDeCusto(unidade);
        return avaliador.pontosDeEntrada().stream()
                .map(avaliador::custoDoMetodo)
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }

    private List<MethodDeclaration> pontosDeEntrada() {
        Optional<MethodDeclaration> principal = metodos.values().stream()
                .filter(metodo -> metodo.getNameAsString().equals("main"))
                .findFirst();
        if (principal.isPresent()) {
            return List.of(principal.get());
        }
        Set<String> chamados = metodosChamados();
        List<MethodDeclaration> raizes = metodos.values().stream()
                .filter(metodo -> !chamados.contains(AstUtils.chaveDoMetodo(metodo)))
                .toList();
        return raizes.isEmpty() ? List.copyOf(metodos.values()) : raizes;
    }

    /** Metodos chamados por <b>outro</b> metodo; auto-chamadas nao tiram um metodo da raiz. */
    private Set<String> metodosChamados() {
        Set<String> chamados = new HashSet<>();
        for (MethodDeclaration metodo : metodos.values()) {
            metodo.findAll(MethodCallExpr.class).stream()
                    .filter(this::ehChamadaLocal)
                    .filter(chamada -> !AstUtils.ehAutoChamada(chamada, metodo))
                    .forEach(chamada -> chamados.add(chaveDaChamada(chamada)));
        }
        return chamados;
    }

    private CustoAvaliado custoDoMetodo(MethodDeclaration metodo) {
        String chave = AstUtils.chaveDoMetodo(metodo);
        CustoAvaliado memorizado = memo.get(chave);
        if (memorizado != null) {
            return memorizado;
        }
        if (emAvaliacao.contains(chave)) {
            return CustoAvaliado.desconhecido(
                    "recursao mutua envolvendo `%s`: nao ha recorrencia de um metodo so a resolver"
                            .formatted(metodo.getNameAsString()));
        }
        emAvaliacao.push(chave);
        try {
            CustoAvaliado corpo = metodo.getBody()
                    .map(bloco -> custoDoNo(bloco, metodo))
                    .orElseGet(() -> CustoAvaliado.exato(Custo.CONSTANTE));
            CustoAvaliado resultado = AnalisadorDeRecursao.ehRecursivo(metodo)
                    ? SolucionadorDeRecorrencia.resolver(AnalisadorDeRecursao.analisar(metodo), corpo)
                    : corpo;
            memo.put(chave, resultado);
            return resultado;
        } finally {
            emAvaliacao.pop();
        }
    }

    private CustoAvaliado custoDoNo(Node no, MethodDeclaration metodoAtual) {
        if (no instanceof ForStmt paraLaco) {
            return custoDeFor(paraLaco, metodoAtual);
        }
        if (no instanceof ForEachStmt paraCada) {
            return custoDoNo(paraCada.getIterable(), metodoAtual)
                    .mais(IteracoesDeLaco.de(paraCada).vezes(custoDoNo(paraCada.getBody(), metodoAtual)));
        }
        if (no instanceof WhileStmt enquantoLaco) {
            CustoAvaliado volta = custoDoNo(enquantoLaco.getCondition(), metodoAtual)
                    .mais(custoDoNo(enquantoLaco.getBody(), metodoAtual));
            return IteracoesDeLaco.de(enquantoLaco).vezes(volta);
        }
        if (no instanceof DoStmt facaLaco) {
            CustoAvaliado volta = custoDoNo(facaLaco.getCondition(), metodoAtual)
                    .mais(custoDoNo(facaLaco.getBody(), metodoAtual));
            return IteracoesDeLaco.de(facaLaco).vezes(volta);
        }
        if (no instanceof IfStmt condicional) {
            CustoAvaliado entao = custoDoNo(condicional.getThenStmt(), metodoAtual);
            CustoAvaliado senao = condicional.getElseStmt()
                    .map(ramo -> custoDoNo(ramo, metodoAtual))
                    .orElseGet(() -> CustoAvaliado.exato(Custo.CONSTANTE));
            return custoDoNo(condicional.getCondition(), metodoAtual).mais(entao.mais(senao));
        }
        if (no instanceof ConditionalExpr ternario) {
            return custoDoNo(ternario.getCondition(), metodoAtual)
                    .mais(custoDoNo(ternario.getThenExpr(), metodoAtual)
                            .mais(custoDoNo(ternario.getElseExpr(), metodoAtual)));
        }
        if (no instanceof SwitchStmt escolha) {
            return custoDoNo(escolha.getSelector(), metodoAtual)
                    .mais(maiorCustoDosFilhos(escolha.getEntries(), metodoAtual));
        }
        if (no instanceof MethodCallExpr chamada) {
            CustoAvaliado escopo = chamada.getScope()
                    .map(receptor -> custoDoNo(receptor, metodoAtual))
                    .orElseGet(() -> CustoAvaliado.exato(Custo.CONSTANTE));
            return escopo.mais(maiorCustoDosFilhos(chamada.getArguments(), metodoAtual))
                    .mais(custoDaChamada(chamada, metodoAtual));
        }
        if (no instanceof ArrayCreationExpr criacao) {
            return maiorCustoDosFilhos(criacao.getChildNodes(), metodoAtual).mais(custoDeAlocacao(criacao));
        }
        if (no instanceof AssignExpr atribuicao) {
            return maiorCustoDosFilhos(atribuicao.getChildNodes(), metodoAtual).mais(custoDeConcatenacao(atribuicao));
        }
        return maiorCustoDosFilhos(no.getChildNodes(), metodoAtual);
    }

    private CustoAvaliado custoDeFor(ForStmt paraLaco, MethodDeclaration metodoAtual) {
        CustoAvaliado inicializacao = maiorCustoDosFilhos(paraLaco.getInitialization(), metodoAtual);
        CustoAvaliado volta = maiorCustoDosFilhos(paraLaco.getUpdate(), metodoAtual)
                .mais(custoDoNo(paraLaco.getBody(), metodoAtual));
        CustoAvaliado comparacao = paraLaco.getCompare()
                .map(condicao -> custoDoNo(condicao, metodoAtual))
                .orElseGet(() -> CustoAvaliado.exato(Custo.CONSTANTE));
        return inicializacao.mais(IteracoesDeLaco.de(paraLaco).vezes(volta.mais(comparacao)));
    }

    /** {@code new int[n]} zera n posicoes; {@code new int[n][n]}, n^2. Dimensoes literais nao dependem da entrada. */
    private CustoAvaliado custoDeAlocacao(ArrayCreationExpr criacao) {
        int dimensoesVariaveis = (int) criacao.getLevels().stream()
                .filter(nivel -> nivel.getDimension().map(AvaliadorDeCusto::dependeDaEntrada).orElse(false))
                .count();
        return CustoAvaliado.exato(Custo.poliLog(dimensoesVariaveis, 0));
    }

    static boolean dependeDaEntrada(Expression tamanho) {
        return !tamanho.isIntegerLiteralExpr() && !tamanho.isLongLiteralExpr();
    }

    /** {@code texto += x} dentro de um laco e a armadilha classica: cada concatenacao copia a String. */
    private CustoAvaliado custoDeConcatenacao(AssignExpr atribuicao) {
        if (atribuicao.getOperator() != AssignExpr.Operator.PLUS || !atribuicao.getTarget().isNameExpr()) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        String alvo = atribuicao.getTarget().asNameExpr().getNameAsString();
        if (!"String".equals(tipos.tipoDe(alvo))) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        return CustoAvaliado.exato(Custo.LINEAR)
                .comNota("concatenacao de String com `+=` copia a cadeia inteira a cada vez");
    }

    private CustoAvaliado custoDaChamada(MethodCallExpr chamada, MethodDeclaration metodoAtual) {
        if (!ehChamadaLocal(chamada)) {
            return TabelaDeCustoApi.custo(chamada, tipos)
                    .orElseGet(() -> CustoAvaliado.estimado(Custo.CONSTANTE,
                            "chamada externa `%s` desconhecida; assumida O(1)".formatted(chamada.getNameAsString())));
        }
        String chave = chaveDaChamada(chamada);
        if (metodoAtual != null && chave.equals(AstUtils.chaveDoMetodo(metodoAtual))) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        MethodDeclaration alvo = metodos.get(chave);
        if (alvo != null) {
            return custoDoMetodo(alvo);
        }
        return CustoAvaliado.estimado(Custo.CONSTANTE,
                "chamada a `%s` nao resolvida nesta unidade; assumida O(1)".formatted(chamada.getNameAsString()));
    }

    private boolean ehChamadaLocal(MethodCallExpr chamada) {
        return chamada.getScope().map(escopo -> escopo instanceof ThisExpr).orElse(true);
    }

    private String chaveDaChamada(MethodCallExpr chamada) {
        return chamada.getNameAsString() + "/" + chamada.getArguments().size();
    }

    private CustoAvaliado maiorCustoDosFilhos(List<? extends Node> filhos, MethodDeclaration metodoAtual) {
        return filhos.stream()
                .map(filho -> custoDoNo(filho, metodoAtual))
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }
}
