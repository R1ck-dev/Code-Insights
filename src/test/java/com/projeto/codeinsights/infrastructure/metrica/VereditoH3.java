package com.projeto.codeinsights.infrastructure.metrica;

/**
 * Conclusao sobre H3 no tempo, por linguagem (Adendo 6, item 7): a taxa de acerto no corpus externo
 * e menor que a do corpus proprio?
 * <p>
 * O veredito usa so a taxa principal e olha a direcao antes do valor-p: queda sem significancia e
 * {@link #INCONCLUSIVO}, nunca "refutada", porque a amostra externa so detecta quedas grandes.
 */
enum VereditoH3 {
    SUSTENTADA,
    INCONCLUSIVO,
    NAO_SUSTENTADA;

    static final double ALFA = 0.05;

    static VereditoH3 de(long acertosProprio, long nProprio, long acertosExterno, long nExterno) {
        if (nProprio <= 0 || nExterno <= 0) {
            throw new IllegalArgumentException("O veredito exige denominadores positivos.");
        }
        if (!taxaExternaMenor(acertosProprio, nProprio, acertosExterno, nExterno)) {
            return NAO_SUSTENTADA;
        }
        double p = EstatisticaExata.fisherBilateral(acertosProprio, nProprio, acertosExterno, nExterno);
        return p < ALFA ? SUSTENTADA : INCONCLUSIVO;
    }

    /** Produto cruzado em vez de divisao: e/nE < a/nP sem passar por {@code double}. */
    private static boolean taxaExternaMenor(long acertosProprio, long nProprio, long acertosExterno,
            long nExterno) {
        return acertosExterno * nProprio < acertosProprio * nExterno;
    }
}
