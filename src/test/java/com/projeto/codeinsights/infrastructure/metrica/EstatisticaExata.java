package com.projeto.codeinsights.infrastructure.metrica;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * As duas contas do Adendo 6 do criterio do corpus externo, em Java puro e sem biblioteca: o
 * intervalo de Wilson de 95% e o teste exato de Fisher bilateral.
 * <p>
 * O Fisher e calculado em inteiros ({@link BigInteger}): a comparacao "tabela tao ou menos provavel
 * que a observada" nao pode depender de tolerancia de ponto flutuante, porque duas tabelas
 * empatadas em probabilidade tem de entrar as duas na soma. A divisao fica para o fim.
 */
final class EstatisticaExata {

    /** Quantil de 97,5% da normal padrao: o z do intervalo de 95%. */
    static final double Z_95 = 1.959963984540054;

    /** Limites do intervalo de confianca, entre 0 e 1. */
    record Intervalo(double inferior, double superior) {
    }

    private EstatisticaExata() {
    }

    /** Intervalo de Wilson de {@code k} acertos em {@code n} tentativas. */
    static Intervalo wilson(long k, long n) {
        if (n <= 0 || k < 0 || k > n) {
            throw new IllegalArgumentException("Wilson exige 0 <= k <= n e n > 0 (k=%d, n=%d).".formatted(k, n));
        }
        double p = (double) k / n;
        double z2 = Z_95 * Z_95;
        double denominador = 1 + z2 / n;
        double centro = (p + z2 / (2.0 * n)) / denominador;
        double meiaLargura = Z_95 * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / denominador;
        return new Intervalo(Math.max(0.0, centro - meiaLargura), Math.min(1.0, centro + meiaLargura));
    }

    /**
     * Valor-p do teste exato de Fisher bilateral para a tabela 2x2 {@code a} acertos em {@code n1}
     * contra {@code b} acertos em {@code n2}: a soma das tabelas de mesmas margens cuja
     * probabilidade e menor ou igual a da observada, sobre o total de tabelas.
     */
    static double fisherBilateral(long a, long n1, long b, long n2) {
        if (n1 < 0 || n2 < 0 || a < 0 || a > n1 || b < 0 || b > n2) {
            throw new IllegalArgumentException(
                    "Tabela invalida (a=%d, n1=%d, b=%d, n2=%d).".formatted(a, n1, b, n2));
        }
        long acertos = a + b;
        BigInteger pesoObservado = peso(n1, n2, acertos, a);
        BigInteger soma = BigInteger.ZERO;
        for (long x = Math.max(0, acertos - n2); x <= Math.min(n1, acertos); x++) {
            BigInteger peso = peso(n1, n2, acertos, x);
            if (peso.compareTo(pesoObservado) <= 0) {
                soma = soma.add(peso);
            }
        }
        return new BigDecimal(soma)
                .divide(new BigDecimal(binomial(n1 + n2, acertos)), MathContext.DECIMAL64)
                .doubleValue();
    }

    /** Probabilidade hipergeometrica da tabela com {@code x} acertos no primeiro grupo, vezes C(N,K). */
    private static BigInteger peso(long n1, long n2, long acertos, long x) {
        return binomial(n1, x).multiply(binomial(n2, acertos - x));
    }

    private static BigInteger binomial(long n, long k) {
        BigInteger resultado = BigInteger.ONE;
        long menor = Math.min(k, n - k);
        for (long i = 1; i <= menor; i++) {
            resultado = resultado.multiply(BigInteger.valueOf(n - menor + i)).divide(BigInteger.valueOf(i));
        }
        return resultado;
    }
}
