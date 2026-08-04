package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.ArrayList;
import java.util.List;

/**
 * No da arvore <b>estrutural</b> de um programa em C: o fluxo de controle, e nada alem dele.
 * <p>
 * Nao e uma AST de C. Uma AST de verdade precisaria resolver <i>typedef</i> para saber se
 * {@code T * x;} e declaracao ou multiplicacao, e precisaria de precedencia de operadores — duas
 * coisas que o modelo de custo <b>nao usa</b>. O que ele usa e: onde ha laco, quantas vezes esse
 * laco gira, o que ha dentro dele, quem chama quem, e o que foi alocado. Essa e exatamente a
 * informacao que esta arvore carrega, e o motivo de ela caber em um parser de descida recursiva em
 * vez de uma gramatica completa de C.
 * <p>
 * As expressoes ficam como <b>texto cru</b> ({@code condicao}, {@code inicio}, {@code passo}). O
 * avaliador de custo varre esse texto atras de chamadas e alocacoes. Guardar texto e uma decisao,
 * nao uma preguica: o custo de uma expressao em C depende de quais funcoes ela chama, e um
 * varredor de chamadas sobre o texto responde isso sem precisar de arvore de expressao.
 *
 * @param inicio    inicializacao do {@code for} (vazia nos demais)
 * @param condicao  a condicao do laco/{@code if}, o seletor do {@code switch}, ou — em
 *                  {@link Tipo#COMANDO} — o comando inteiro
 * @param passo     atualizacao do {@code for} (vazia nos demais)
 * @param filhos    corpo. Em {@link Tipo#IF} sao os ramos (entao, e senao quando existe); como o
 *                  custo de uma sequencia ja e o <b>maior</b> dos filhos, ramos e sequencia
 *                  compoem pela mesma regra e nao precisam de tratamento separado.
 */
public record NoDeC(Tipo tipo, String inicio, String condicao, String passo, List<NoDeC> filhos) {

    public enum Tipo {
        BLOCO,
        FOR,
        WHILE,
        DO,
        IF,
        SWITCH,
        COMANDO,
        /**
         * {@code case 3:}, {@code default:} ou rotulo de {@code goto}. Nao custa nada, mas precisa
         * existir na arvore: e ele que marca onde um {@code case} termina e o proximo comeca, e sem
         * essa fronteira as auto-chamadas de todos os {@code case} seguintes ao primeiro que
         * retorna sumiam da contagem de recursao.
         */
        ROTULO
    }

    private static final String VAZIO = "";

    public static NoDeC bloco(List<NoDeC> filhos) {
        return new NoDeC(Tipo.BLOCO, VAZIO, VAZIO, VAZIO, List.copyOf(filhos));
    }

    public static NoDeC comando(String texto) {
        return new NoDeC(Tipo.COMANDO, VAZIO, texto, VAZIO, List.of());
    }

    public static NoDeC rotulo(String nome) {
        return new NoDeC(Tipo.ROTULO, VAZIO, nome, VAZIO, List.of());
    }

    public static NoDeC para(String inicio, String condicao, String passo, NoDeC corpo) {
        return new NoDeC(Tipo.FOR, inicio, condicao, passo, List.of(corpo));
    }

    public static NoDeC enquanto(String condicao, NoDeC corpo) {
        return new NoDeC(Tipo.WHILE, VAZIO, condicao, VAZIO, List.of(corpo));
    }

    public static NoDeC facaEnquanto(String condicao, NoDeC corpo) {
        return new NoDeC(Tipo.DO, VAZIO, condicao, VAZIO, List.of(corpo));
    }

    public static NoDeC condicional(String condicao, List<NoDeC> ramos) {
        return new NoDeC(Tipo.IF, VAZIO, condicao, VAZIO, List.copyOf(ramos));
    }

    public static NoDeC escolha(String seletor, NoDeC corpo) {
        return new NoDeC(Tipo.SWITCH, VAZIO, seletor, VAZIO, List.of(corpo));
    }

    public boolean ehLaco() {
        return tipo == Tipo.FOR || tipo == Tipo.WHILE || tipo == Tipo.DO;
    }

    /** Todo o texto de expressao deste no, sem o dos filhos. */
    public String textoProprio() {
        return inicio + " " + condicao + " " + passo;
    }

    /** Este no e toda a sua descendencia, em pre-ordem. */
    public List<NoDeC> comDescendentes() {
        List<NoDeC> todos = new ArrayList<>();
        acumular(todos);
        return todos;
    }

    private void acumular(List<NoDeC> destino) {
        destino.add(this);
        filhos.forEach(filho -> filho.acumular(destino));
    }

    /** Todo o texto de expressao desta subarvore — usado para procurar chamadas e alocacoes. */
    public String textoDaSubarvore() {
        StringBuilder texto = new StringBuilder();
        comDescendentes().forEach(no -> texto.append(no.textoProprio()).append(' '));
        return texto.toString();
    }
}
