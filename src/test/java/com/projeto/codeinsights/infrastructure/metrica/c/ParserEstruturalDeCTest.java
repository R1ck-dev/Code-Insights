package com.projeto.codeinsights.infrastructure.metrica.c;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.infrastructure.metrica.c.ProgramaDeC.FuncaoDeC;

/**
 * O parser estrutural e a base de Big-O e espaco em C: se ele ler a estrutura errado, as duas
 * metricas erram juntas e em silencio. Estes testes prendem o que ele reconhece — e, tao
 * importante quanto, o que ele <b>recusa</b> em vez de adivinhar.
 */
class ParserEstruturalDeCTest {

    @Test
    @DisplayName("separa as funcoes do arquivo, com nome e parametros")
    void separaAsFuncoes() {
        ProgramaDeC programa = ParserEstruturalDeC.analisar("""
                #include <stdio.h>

                int soma(int a, int b) { return a + b; }

                int main(void) {
                    printf("%d", soma(1, 2));
                    return 0;
                }
                """);

        assertThat(programa.integro()).isTrue();
        assertThat(programa.funcoes()).extracting(FuncaoDeC::chave)
                .containsExactly("soma/2", "main/0");
        assertThat(programa.porNome("soma")).get()
                .extracting(FuncaoDeC::parametros).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .containsExactly("a", "b");
    }

    @Test
    @DisplayName("struct e vetor inicializado nao viram funcao")
    void naoConfundeChaveDeStructComCorpoDeFuncao() {
        ProgramaDeC programa = ParserEstruturalDeC.analisar("""
                struct Ponto { int x; int y; };
                int tabela[3] = {1, 2, 3};

                int main(void) { return tabela[0]; }
                """);

        assertThat(programa.funcoes()).extracting(FuncaoDeC::nome).containsExactly("main");
    }

    @Test
    @DisplayName("le o cabecalho do for em tres partes")
    void separaInicioCondicaoEPasso() {
        NoDeC laco = primeiroLaco("int main(void) { for (int i = 0; i < n; i++) { total += i; } }");

        assertThat(laco.tipo()).isEqualTo(NoDeC.Tipo.FOR);
        assertThat(laco.inicio()).isEqualTo("int i = 0");
        assertThat(laco.condicao()).isEqualTo("i < n");
        assertThat(laco.passo()).isEqualTo("i++");
    }

    @Test
    @DisplayName("laco dentro de laco vira laco dentro de laco, e nao dois irmaos")
    void aninhaOsLacos() {
        NoDeC externo = primeiroLaco("""
                int main(void) {
                    for (i = 0; i < n; i++)
                        for (j = 0; j < n; j++)
                            v[i][j] = 0;
                }
                """);

        assertThat(externo.comDescendentes()).filteredOn(NoDeC::ehLaco).hasSize(2);
        assertThat(externo.filhos().get(0).tipo()).isEqualTo(NoDeC.Tipo.FOR);
    }

    @Test
    @DisplayName("if/else guarda os dois ramos; if solto guarda um so")
    void distingueOsRamosDoCondicional() {
        ProgramaDeC programa = ParserEstruturalDeC.analisar("""
                int main(void) {
                    if (a > b) { x = 1; } else { x = 2; }
                    if (c) { y = 3; }
                }
                """);

        var condicionais = programa.funcoes().get(0).corpo().comDescendentes().stream()
                .filter(no -> no.tipo() == NoDeC.Tipo.IF)
                .toList();

        assertThat(condicionais).hasSize(2);
        assertThat(condicionais.get(0).filhos()).hasSize(2);
        assertThat(condicionais.get(1).filhos()).hasSize(1);
    }

    @Test
    @DisplayName("do-while le a condicao que vem depois do corpo")
    void leDoWhile() {
        NoDeC laco = primeiroLaco("int main(void) { do { i++; } while (i < n); }");

        assertThat(laco.tipo()).isEqualTo(NoDeC.Tipo.DO);
        assertThat(laco.condicao()).isEqualTo("i < n");
    }

    @Test
    @DisplayName("switch: rotulo de case nao vira comando, o corpo dele vira")
    void leSwitch() {
        ProgramaDeC programa = ParserEstruturalDeC.analisar("""
                int main(void) {
                    switch (x) {
                        case 1: return f(1);
                        case 2: return g(2);
                        default: return 0;
                    }
                }
                """);

        NoDeC escolha = programa.funcoes().get(0).corpo().comDescendentes().stream()
                .filter(no -> no.tipo() == NoDeC.Tipo.SWITCH)
                .findFirst()
                .orElseThrow();

        assertThat(escolha.condicao()).isEqualTo("x");
        assertThat(escolha.textoDaSubarvore()).contains("f(1)").contains("g(2)");
    }

    /**
     * Uma chave a menos engoliria o resto do arquivo dentro do ultimo laco e produziria um Big-O
     * inventado. Desistir e o unico jeito de isso nao passar por medicao.
     */
    @Test
    @DisplayName("chave sem par derruba a analise em vez de produzir uma arvore truncada")
    void recusaEstruturaQuebrada() {
        ProgramaDeC programa = ParserEstruturalDeC.analisar("""
                int main(void) {
                    for (i = 0; i < n; i++) {
                        total += i;
                }
                """);

        assertThat(programa.integro()).isFalse();
        assertThat(programa.motivo()).isNotBlank();
    }

    @Test
    @DisplayName("arquivo sem nenhuma funcao nao e programa")
    void recusaArquivoSemFuncao() {
        assertThat(ParserEstruturalDeC.analisar("int global = 3;").integro()).isFalse();
        assertThat(ParserEstruturalDeC.analisar("").integro()).isFalse();
    }

    /** O lexer ja tirou string e comentario; o parser nao pode ressuscita-los como estrutura. */
    @Test
    @DisplayName("chave dentro de string ou comentario nao abre bloco")
    void ignoraChaveQueNaoEhCodigo() {
        ProgramaDeC programa = ParserEstruturalDeC.analisar("""
                int main(void) {
                    printf("{ nao abre bloco");
                    /* } nao fecha nada */
                    return 0;
                }
                """);

        assertThat(programa.integro()).isTrue();
        assertThat(programa.funcoes()).hasSize(1);
    }

    private static NoDeC primeiroLaco(String fonte) {
        return ParserEstruturalDeC.analisar(fonte).funcoes().get(0).corpo().comDescendentes().stream()
                .filter(NoDeC::ehLaco)
                .findFirst()
                .orElseThrow();
    }
}
