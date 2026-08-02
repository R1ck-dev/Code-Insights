package com.projeto.codeinsights.domain.pesquisa.enums;

/**
 * O que o participante respondeu ao Termo de Consentimento Livre e Esclarecido.
 * <p>
 * Sao dois valores, e nao tres: <b>revogacao nao e um valor separado</b>, e sim uma
 * {@link #RECUSA} registrada depois de um {@link #ACEITE} para a mesma versao do termo. O historico
 * e append-only, entao a sequencia ja conta a historia — "aceitou em 12/03, recusou em 20/04" e
 * literalmente o que esta gravado. Um terceiro valor derivavel dos outros dois so criaria a chance
 * de ele divergir do que a ordem dos registros diz.
 */
public enum DecisaoDeConsentimento {

    /** Autoriza o uso das proprias submissoes na pesquisa. */
    ACEITE,

    /** Nao autoriza. Se houver um ACEITE anterior para a mesma versao, isto e uma revogacao. */
    RECUSA
}
