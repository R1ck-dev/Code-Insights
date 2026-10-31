package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.IdentityHashMap;
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
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Recursao;

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

    private final ResolvedorDeChamadas resolvedor;
    private final TiposDeVariavel tipos;
    private final ConstantesFinais constantes;
    private final Map<MethodDeclaration, CustoAvaliado> memo = new IdentityHashMap<>();
    private final Map<MethodDeclaration, List<MethodDeclaration>> ciclos;
    private final Set<MethodDeclaration> emAvaliacao = ResolvedorDeChamadas.conjuntoPorIdentidade();

    private AvaliadorDeCusto(CompilationUnit unidade) {
        this.resolvedor = ResolvedorDeChamadas.de(unidade);
        this.tipos = resolvedor.tipos();
        this.constantes = ConstantesFinais.de(unidade);
        this.ciclos = GrafoDeChamadas.ciclos(resolvedor);
    }

    public static CustoAvaliado doPrograma(CompilationUnit unidade) {
        AvaliadorDeCusto avaliador = new AvaliadorDeCusto(unidade);
        return avaliador.pontosDeEntrada().stream()
                .map(avaliador::custoDoMetodo)
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }

    private List<MethodDeclaration> pontosDeEntrada() {
        List<MethodDeclaration> metodos = resolvedor.metodos();
        Optional<MethodDeclaration> principal = metodos.stream()
                .filter(metodo -> metodo.getNameAsString().equals("main"))
                .findFirst();
        if (principal.isPresent()) {
            return List.of(principal.get());
        }
        Set<MethodDeclaration> chamados = metodosChamados();
        List<MethodDeclaration> raizes = metodos.stream()
                .filter(metodo -> !chamados.contains(metodo))
                .toList();
        return raizes.isEmpty() ? metodos : raizes;
    }

    /**
     * Metodos chamados ou referenciados ({@code Main::f}) por <b>outro</b> metodo; auto-chamadas nao tiram
     * um metodo da raiz. Sem {@code main}, um metodo so referenciado e custeado pela referencia, nao como raiz.
     */
    private Set<MethodDeclaration> metodosChamados() {
        Set<MethodDeclaration> chamados = ResolvedorDeChamadas.conjuntoPorIdentidade();
        resolvedor.metodos().forEach(metodo -> {
            chamados.addAll(resolvedor.chamadosPor(metodo));
            chamados.addAll(resolvedor.referenciadosPor(metodo));
        });
        return chamados;
    }

    private CustoAvaliado custoDoMetodo(MethodDeclaration metodo) {
        CustoAvaliado memorizado = memo.get(metodo);
        if (memorizado != null) {
            return memorizado;
        }
        if (emAvaliacao.contains(metodo)) {
            return CustoAvaliado.desconhecido(
                    "recursao mutua envolvendo `%s`: nao ha recorrencia de um metodo so a resolver"
                            .formatted(metodo.getNameAsString()));
        }
        if (ciclos.containsKey(metodo)) {
            return custoDoCiclo(ciclos.get(metodo));
        }
        emAvaliacao.add(metodo);
        try {
            CustoAvaliado corpo = custoLocal(metodo);
            CustoAvaliado resultado = AnalisadorDeRecursao.ehRecursivo(metodo, resolvedor)
                    ? SolucionadorDeRecorrencia.resolver(AnalisadorDeRecursao.analisar(metodo, resolvedor), corpo)
                    : corpo;
            memo.put(metodo, resultado);
            return resultado;
        } finally {
            emAvaliacao.remove(metodo);
        }
    }

    private CustoAvaliado custoLocal(MethodDeclaration metodo) {
        return metodo.getBody()
                .map(bloco -> custoDoNo(bloco, metodo))
                .orElseGet(() -> CustoAvaliado.exato(Custo.CONSTANTE));
    }

    /**
     * Recursao mutua: uma recorrencia so para o ciclo inteiro ({@link CicloDeRecursao}). O custo local de cada
     * membro conta as chamadas ao ciclo como O(1) ({@link #custoDaChamada}); o resultado e o mesmo para todos
     * os membros, entao e gravado no memo de cada um. Sem {@code main} e sem raiz fora do ciclo, todos os
     * membros sao pontos de entrada ({@link #pontosDeEntrada}) e o custo e o do ciclo, para qualquer entrada.
     */
    private CustoAvaliado custoDoCiclo(List<MethodDeclaration> ciclo) {
        emAvaliacao.addAll(ciclo);
        try {
            List<String> nomes = ciclo.stream().map(MethodDeclaration::getNameAsString).toList();
            List<CustoAvaliado> locais = ciclo.stream().map(this::custoLocal).toList();
            List<Recursao> recursoes = ciclo.stream()
                    .map(membro -> AnalisadorDeRecursao.analisarNoCiclo(membro, ciclo, resolvedor))
                    .toList();
            CustoAvaliado resultado = CicloDeRecursao.combinar(recursoes)
                    .map(unica -> CicloDeRecursao.resolver(nomes, unica, locais))
                    .orElseGet(() -> CicloDeRecursao.semRecorrenciaUnica(nomes, recursoes));
            ciclo.forEach(membro -> memo.put(membro, resultado));
            return resultado;
        } finally {
            ciclo.forEach(emAvaliacao::remove);
        }
    }

    private CustoAvaliado custoDoNo(Node no, MethodDeclaration metodoAtual) {
        if (no instanceof ForStmt paraLaco) {
            return custoDeFor(paraLaco, metodoAtual);
        }
        if (no instanceof ForEachStmt paraCada) {
            return custoDoNo(paraCada.getIterable(), metodoAtual)
                    .mais(IteracoesDeLaco.de(paraCada, constantes).vezes(custoDoNo(paraCada.getBody(), metodoAtual)));
        }
        if (no instanceof WhileStmt enquantoLaco) {
            CustoAvaliado volta = custoDoNo(enquantoLaco.getCondition(), metodoAtual)
                    .mais(custoDoNo(enquantoLaco.getBody(), metodoAtual));
            return IteracoesDeLaco.de(enquantoLaco, constantes).vezes(volta);
        }
        if (no instanceof DoStmt facaLaco) {
            CustoAvaliado volta = custoDoNo(facaLaco.getCondition(), metodoAtual)
                    .mais(custoDoNo(facaLaco.getBody(), metodoAtual));
            return IteracoesDeLaco.de(facaLaco, constantes).vezes(volta);
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
            return custoDeChamadaComArgumentos(chamada, metodoAtual);
        }
        if (no instanceof MethodReferenceExpr referencia) {
            return custoDoNo(referencia.getScope(), metodoAtual).mais(custoDoMetodoReferenciado(referencia, metodoAtual));
        }
        if (no instanceof ArrayCreationExpr criacao) {
            return maiorCustoDosFilhos(criacao.getChildNodes(), metodoAtual).mais(custoDeAlocacao(criacao));
        }
        if (no instanceof AssignExpr atribuicao) {
            return maiorCustoDosFilhos(atribuicao.getChildNodes(), metodoAtual).mais(custoDeConcatenacao(atribuicao));
        }
        return maiorCustoDosFilhos(no.getChildNodes(), metodoAtual);
    }

    private CustoAvaliado custoDeChamadaComArgumentos(MethodCallExpr chamada, MethodDeclaration metodoAtual) {
        CustoAvaliado escopo = chamada.getScope()
                .map(receptor -> custoDoNo(receptor, metodoAtual))
                .orElseGet(() -> CustoAvaliado.exato(Custo.CONSTANTE));
        return escopo.mais(custoDosArgumentos(chamada, metodoAtual))
                .mais(custoDaChamada(chamada, metodoAtual));
    }

    /**
     * Argumentos da chamada. O lambda — ou a referencia a metodo — de uma operacao que percorre os elementos
     * ({@link OperacoesComCallback}) roda uma vez por elemento: seu custo <b>multiplica</b>; qualquer outro
     * argumento entra por {@code mais}.
     */
    private CustoAvaliado custoDosArgumentos(MethodCallExpr chamada, MethodDeclaration metodoAtual) {
        Optional<CustoAvaliado> repeticoes = OperacoesComCallback.repeticoes(chamada, resolvedor);
        return chamada.getArguments().stream()
                .map(argumento -> custoDeArgumento(argumento, repeticoes, metodoAtual))
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }

    private CustoAvaliado custoDeArgumento(Expression argumento, Optional<CustoAvaliado> repeticoes,
            MethodDeclaration metodoAtual) {
        if (repeticoes.isPresent() && argumento instanceof MethodReferenceExpr referencia) {
            // O receptor (`obj` em obj::m) e avaliado uma vez; so o metodo apontado roda por elemento.
            return custoDoNo(referencia.getScope(), metodoAtual)
                    .mais(repeticoes.get().vezes(custoDoMetodoReferenciado(referencia, metodoAtual)));
        }
        CustoAvaliado custo = custoDoNo(argumento, metodoAtual);
        return repeticoes.isPresent() && argumento instanceof LambdaExpr
                ? repeticoes.get().vezes(custo)
                : custo;
    }

    /**
     * Custo de uma execucao do metodo da unidade que a referencia aponta. Biblioteca ({@code Integer::sum}),
     * construtor e receptor de tipo desconhecido saem O(1), como uma chamada externa. Mais de uma sobrecarga
     * de mesmo nome: vale a de <b>maior</b> custo, com nota — a referencia nao diz qual aridade o callback usa.
     * Referencia ao proprio metodo (ou a um ciclo) e recursao por dentro de callback, que o motor nao
     * resolve: sai "?", como a recursao mutua.
     */
    private CustoAvaliado custoDoMetodoReferenciado(MethodReferenceExpr referencia, MethodDeclaration metodoAtual) {
        List<MethodDeclaration> alvos = resolvedor.alvosDaReferencia(referencia);
        if (alvos.isEmpty()) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        if (alvos.stream().anyMatch(alvo -> alvo == metodoAtual)) {
            return CustoAvaliado.desconhecido(
                    "referencia `%s` ao proprio metodo: recursao por dentro de callback nao e analisada"
                            .formatted(referencia.getIdentifier()));
        }
        CustoAvaliado maior = alvos.stream()
                .map(this::custoDeUmaExecucao)
                .reduce(CustoAvaliado::mais)
                .orElseThrow();
        return alvos.size() > 1
                ? maior.rebaixado(NivelConfianca.MEDIA).comNota("referencia `%s` com %d sobrecargas: assumida a de maior custo"
                        .formatted(referencia.getIdentifier(), alvos.size()))
                : maior;
    }

    private CustoAvaliado custoDeUmaExecucao(MethodDeclaration metodo) {
        return metodo.getBody().isEmpty()
                ? CustoAvaliado.estimado(Custo.CONSTANTE,
                        "metodo abstrato `%s`: o custo das implementacoes nao e considerado"
                                .formatted(metodo.getNameAsString()))
                : custoDoMetodo(metodo);
    }

    private CustoAvaliado custoDeFor(ForStmt paraLaco, MethodDeclaration metodoAtual) {
        CustoAvaliado inicializacao = maiorCustoDosFilhos(paraLaco.getInitialization(), metodoAtual);
        CustoAvaliado volta = maiorCustoDosFilhos(paraLaco.getUpdate(), metodoAtual)
                .mais(custoDoNo(paraLaco.getBody(), metodoAtual));
        CustoAvaliado comparacao = paraLaco.getCompare()
                .map(condicao -> custoDoNo(condicao, metodoAtual))
                .orElseGet(() -> CustoAvaliado.exato(Custo.CONSTANTE));
        return inicializacao.mais(IteracoesDeLaco.de(paraLaco, constantes).vezes(volta.mais(comparacao)));
    }

    /** {@code new int[n]} zera n posicoes; {@code new int[n][n]}, n^2. Dimensoes literais ou constantes finais nao dependem da entrada. */
    private CustoAvaliado custoDeAlocacao(ArrayCreationExpr criacao) {
        int dimensoesVariaveis = (int) criacao.getLevels().stream()
                .filter(nivel -> nivel.getDimension().map(tamanho -> dependeDaEntrada(tamanho, constantes)).orElse(false))
                .count();
        return CustoAvaliado.exato(Custo.poliLog(dimensoesVariaveis, 0));
    }

    /** Tamanho que nao e literal nem constante final ({@link ConstantesFinais}). */
    static boolean dependeDaEntrada(Expression tamanho, ConstantesFinais constantes) {
        return !constantes.ehConstante(tamanho);
    }

    /** So o literal; para quem nao tem as constantes da unidade a mao (faixa de {@code IntStream.range}). */
    static boolean dependeDaEntrada(Expression tamanho) {
        return !tamanho.isIntegerLiteralExpr() && !tamanho.isLongLiteralExpr();
    }

    /**
     * {@code texto += x} — ou {@code texto = texto + x} — dentro de um laco e a armadilha
     * classica: cada concatenacao copia a String inteira. As duas formas contam, porque
     * significam a mesma coisa e o iniciante escreve a segunda.
     */
    private CustoAvaliado custoDeConcatenacao(AssignExpr atribuicao) {
        boolean acumulaTexto = AstUtils.alvoDeAcumulacao(atribuicao)
                .map(alvo -> "String".equals(tipos.tipoDe(alvo)))
                .orElse(false);
        if (!acumulaTexto) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        return CustoAvaliado.exato(Custo.LINEAR)
                .comNota("concatenacao de String copia a cadeia inteira a cada vez");
    }

    private CustoAvaliado custoDaChamada(MethodCallExpr chamada, MethodDeclaration metodoAtual) {
        Optional<MethodDeclaration> alvo = resolvedor.alvo(chamada);
        if (alvo.isEmpty()) {
            return ehChamadaLocal(chamada) ? naoResolvida(chamada) : daBiblioteca(chamada);
        }
        if (alvo.get() == metodoAtual || mesmoCiclo(alvo.get(), metodoAtual)) {
            return CustoAvaliado.exato(Custo.CONSTANTE);
        }
        if (alvo.get().getBody().isEmpty()) {
            return CustoAvaliado.estimado(Custo.CONSTANTE,
                    "metodo abstrato `%s`: o custo das implementacoes nao e considerado"
                            .formatted(chamada.getNameAsString()));
        }
        return custoDoMetodo(alvo.get());
    }

    private boolean mesmoCiclo(MethodDeclaration alvo, MethodDeclaration metodoAtual) {
        List<MethodDeclaration> ciclo = ciclos.get(metodoAtual);
        return ciclo != null && ciclo.stream().anyMatch(membro -> membro == alvo);
    }

    private CustoAvaliado daBiblioteca(MethodCallExpr chamada) {
        return TabelaDeCustoApi.custo(chamada, resolvedor)
                .orElseGet(() -> CustoAvaliado.estimado(Custo.CONSTANTE,
                        "chamada externa `%s` desconhecida; assumida O(1)".formatted(chamada.getNameAsString())));
    }

    private CustoAvaliado naoResolvida(MethodCallExpr chamada) {
        return CustoAvaliado.estimado(Custo.CONSTANTE,
                "chamada a `%s` nao resolvida nesta unidade; assumida O(1)".formatted(chamada.getNameAsString()));
    }

    private boolean ehChamadaLocal(MethodCallExpr chamada) {
        return chamada.getScope().map(escopo -> escopo instanceof ThisExpr).orElse(true);
    }

    private CustoAvaliado maiorCustoDosFilhos(List<? extends Node> filhos, MethodDeclaration metodoAtual) {
        return filhos.stream()
                .map(filho -> custoDoNo(filho, metodoAtual))
                .reduce(CustoAvaliado.exato(Custo.CONSTANTE), CustoAvaliado::mais);
    }
}
