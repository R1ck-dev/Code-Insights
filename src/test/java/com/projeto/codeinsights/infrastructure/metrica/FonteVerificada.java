package com.projeto.codeinsights.infrastructure.metrica;

/**
 * O codigo de um caso externo depois de o SHA-256 dos bytes bater com o manifesto. E o unico objeto
 * do corpus externo que carrega codigo de terceiros, e por isso nao se imprime: o {@code toString()}
 * gerado pelo {@code record} incluiria o fonte inteiro, entao ele e sobrescrito para mostrar so a
 * identificacao do caso.
 *
 * @param caso   a anotacao do caso (sem codigo)
 * @param codigo o fonte, decodificado de bytes ja conferidos
 */
record FonteVerificada(CasoExterno caso, String codigo) {

    @Override
    public String toString() {
        return "FonteVerificada[" + caso + "]";
    }
}
