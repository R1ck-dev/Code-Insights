package com.projeto.codeinsights.infrastructure.metrica.custo;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Recursao;

/**
 * Resolve a recorrencia {@code T(n)} extraida de um metodo recursivo.
 * <p>
 * Para divisao e conquista, {@code T(n) = a T(n/b) + f(n)}, aplica o
 * <b>Teorema Mestre</b> - o mesmo mecanismo classifica merge sort ({@code 2T(n/2)+O(n)}
 * -> {@code n log n}), busca binaria ({@code T(n/2)+O(1)} -> {@code log n}) e percurso
 * de arvore binaria ({@code 2T(n/2)+O(1)} -> {@code n}), sem nenhum caso especial.
 * Para {@code T(n) = a T(n-c) + f(n)}, a recursao desce {@code n/c} niveis: com
 * {@code a = 1} o custo e {@code n * f(n)}; com {@code a >= 2} a arvore e exponencial
 * (Fibonacci ingenuo).
 */
public final class SolucionadorDeRecorrencia {

    private static final double TOLERANCIA = 1e-9;

    private SolucionadorDeRecorrencia() {
    }

    /** @param custoLocal custo do corpo do metodo <b>sem</b> as auto-chamadas, o {@code f(n)} da recorrencia. */
    public static CustoAvaliado resolver(Recursao recursao, CustoAvaliado custoLocal) {
        if (custoLocal.custo().ehDesconhecido()) {
            return custoLocal;
        }
        CustoAvaliado resultado = aplicar(recursao, custoLocal);
        if (!recursao.temSuposicao()) {
            return resultado;
        }
        return resultado.rebaixado(NivelConfianca.MEDIA).comNota(recursao.suposicao());
    }

    private static CustoAvaliado aplicar(Recursao recursao, CustoAvaliado custoLocal) {
        if (recursao.memoizada()) {
            Custo estados = Custo.poliLog(recursao.dimensoesDoCache(), 0);
            return custoLocal.com(estados.vezes(custoLocal.custo()))
                    .comNota("custo = estados distintos do cache (%s) x trabalho por estado (%s)"
                            .formatted(estados.descricao(), custoLocal.custo().descricao()));
        }
        return switch (recursao.reducao()) {
            case DIVISIVA -> teoremaMestre(recursao, custoLocal);
            case SUBTRATIVA -> subtrativa(recursao, custoLocal);
            case ESTRUTURAL -> custoLocal.com(custoLocal.custo().vezes(Custo.LINEAR))
                    .comNota("cada elemento da estrutura e visitado uma vez, ao custo de %s"
                            .formatted(custoLocal.custo().descricao()));
            case BACKTRACKING -> custoLocal.com(Custo.EXPONENCIAL)
                    .comNota("a recursao ramifica dentro de um laco: arvore de busca exponencial");
            case INDETERMINADA -> CustoAvaliado.desconhecido(
                    "recursao cuja reducao de argumento nao foi reconhecida; nao ha recorrencia a resolver");
        };
    }

    /** {@code T(n) = a T(n-c) + f(n)}: sao {@code n/c} niveis, cada um custando {@code f(n)}. */
    private static CustoAvaliado subtrativa(Recursao recursao, CustoAvaliado custoLocal) {
        String recorrencia = "T(n) = %d T(n-%d) + O(%s)"
                .formatted(recursao.chamadasPorCaminho(), recursao.fator(), custoLocal.custo().descricao());
        if (recursao.chamadasPorCaminho() == 1) {
            return custoLocal.com(custoLocal.custo().vezes(Custo.LINEAR))
                    .comNota(recorrencia + ": recursao linear, n niveis");
        }
        return custoLocal.com(Custo.EXPONENCIAL)
                .comNota(recorrencia + ": a arvore de recursao dobra a cada nivel");
    }

    private static CustoAvaliado teoremaMestre(Recursao recursao, CustoAvaliado custoLocal) {
        Custo local = custoLocal.custo();
        if (local.forma() != Custo.Forma.POLILOG) {
            return custoLocal.comNota("o corpo do metodo ja domina a recursao");
        }

        int chamadas = recursao.chamadasPorCaminho();
        int fator = Math.max(recursao.fator(), 2);
        int grauLocal = local.grauPoli();
        double expoente = Math.log(chamadas) / Math.log(fator);
        String recorrencia = "T(n) = %d T(n/%d) + O(%s)".formatted(chamadas, fator, local.descricao());

        if (Math.abs(expoente - grauLocal) < TOLERANCIA) {
            return custoLocal.com(Custo.poliLog(grauLocal, local.grauLog() + 1))
                    .comNota(recorrencia + ": caso 2 do teorema mestre (folhas e raiz custam o mesmo)");
        }
        if (expoente < grauLocal) {
            return custoLocal.comNota(recorrencia + ": caso 3 do teorema mestre (o trabalho fora da recursao domina)");
        }
        return casoDominadoPelasFolhas(custoLocal, chamadas, fator, expoente, recorrencia);
    }

    /** Caso 1: as folhas dominam e o custo e {@code n^log_b(a)}. */
    private static CustoAvaliado casoDominadoPelasFolhas(CustoAvaliado custoLocal, int chamadas, int fator,
            double expoente, String recorrencia) {
        int grau = (int) Math.ceil(expoente - TOLERANCIA);
        CustoAvaliado resultado = custoLocal.com(Custo.poliLog(grau, 0))
                .comNota(recorrencia + ": caso 1 do teorema mestre, custo n^log_%d(%d)".formatted(fator, chamadas));
        if (Math.abs(expoente - Math.rint(expoente)) < TOLERANCIA) {
            return resultado;
        }
        return resultado.rebaixado(NivelConfianca.MEDIA)
                .comNota("expoente log_%d(%d) = %.2f arredondado para cima (%d) para caber na escala"
                        .formatted(fator, chamadas, expoente, grau));
    }
}
