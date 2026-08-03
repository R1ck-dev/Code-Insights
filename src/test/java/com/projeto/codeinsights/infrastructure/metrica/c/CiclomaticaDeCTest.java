package com.projeto.codeinsights.infrastructure.metrica.c;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * A ciclomatica de C precisa dar o MESMO numero que a de Java para o mesmo algoritmo — e a formula
 * {@code M = decisoes + P} so vale se as duas contarem as mesmas coisas. Um erro aqui nao quebra
 * nada visivelmente: ele produz um numero plausivel e errado, que entra no corpus da pesquisa.
 */
class CiclomaticaDeCTest {

    private int m(String fonte) {
        return CiclomaticaDeC.contar(fonte).complexidade();
    }

    @Test
    void funcaoSemRamificacaoTemComplexidadeUm() {
        assertThat(m("int main(void) { return 0; }")).isEqualTo(1);
    }

    @Test
    void cadaIfSomaUm() {
        assertThat(m("int f(int x) { if (x > 0) { return 1; } return 0; }")).isEqualTo(2);
    }

    /** `else` nao ramifica: o caminho alternativo do `if` ja foi contado. */
    @Test
    void elseNaoSomaMasElseIfSoma() {
        assertThat(m("int f(int x) { if (x) return 1; else return 2; }")).isEqualTo(2);
        assertThat(m("int f(int x) { if (x) return 1; else if (x > 2) return 2; return 0; }"))
                .isEqualTo(3);
    }

    /**
     * O caso que um contador ingenuo erra: {@code do ... while} tem as duas palavras e e UM laco.
     * Contar {@code do} e {@code while} daria 3 aqui.
     */
    @Test
    void doWhileContaUmaVezSo() {
        assertThat(m("int f(void) { int i = 0; do { i++; } while (i < 10); return i; }")).isEqualTo(2);
    }

    @Test
    void lacosSomamUmCadaUm() {
        assertThat(m("int f(int n) { for (int i = 0; i < n; i++) { } int j = 0; while (j < n) j++; return j; }"))
                .isEqualTo(3);
    }

    /** Curto-circuito cria caminho: cada `&&`/`||` e um ponto de decisao. */
    @Test
    void operadoresDeCurtoCircuitoSomam() {
        assertThat(m("int f(int a, int b) { if (a > 0 && b > 0 || a == b) return 1; return 0; }"))
                .isEqualTo(4);
    }

    /** Bit a bit nao ramifica — so o par duplo conta. */
    @Test
    void operadorBitABitNaoSoma() {
        assertThat(m("int f(int a, int b) { return a & b | a ^ b; }")).isEqualTo(1);
    }

    /** `case` ramifica; `default` e `switch` nao — mesmo criterio do lado Java. */
    @Test
    void cadaCaseSomaEDefaultNao() {
        assertThat(m("""
                int f(int x) {
                  switch (x) {
                    case 1: return 1;
                    case 2: return 2;
                    default: return 0;
                  }
                }
                """)).isEqualTo(3);
    }

    @Test
    void ternarioSoma() {
        assertThat(m("int f(int x) { return x > 0 ? 1 : -1; }")).isEqualTo(2);
    }

    /** P = numero de funcoes: M = decisoes + P, e nao decisoes + 1. */
    @Test
    void cadaFuncaoAcrescentaUmComponente() {
        assertThat(m("int a(void) { return 0; } int b(void) { return 1; }")).isEqualTo(2);
        assertThat(m("int a(int x) { if (x) return 1; return 0; } int b(void) { return 1; }"))
                .isEqualTo(3);
    }

    /** O que o lexer existe para evitar: texto nao ramifica. */
    @Test
    void palavraChaveDentroDeStringOuComentarioNaoConta() {
        assertThat(m("""
                int main(void) {
                  // if (a && b) for while case
                  /* if (c) { } */
                  printf("if (x) while (y) case 1: a && b || c ? d : e");
                  return 0;
                }
                """)).isEqualTo(1);
    }

    /** Aspas escapadas nao encerram o literal; se encerrassem, o resto do arquivo viraria codigo. */
    @Test
    void escapeDentroDeLiteralNaoEncerraOTexto() {
        assertThat(m("""
                int main(void) {
                  printf("aspas \\" if (x) && (y)");
                  char c = '\\'';
                  return 0;
                }
                """)).isEqualTo(1);
    }

    /**
     * Diretiva e decisao de COMPILACAO: o binario ja saiu com um lado so. Contar as duas inflaria a
     * metrica de qualquer arquivo com guarda de header.
     */
    @Test
    void diretivaDePreProcessadorNaoConta() {
        assertThat(m("""
                #ifndef CABECALHO_H
                #define CABECALHO_H
                #if defined(X) && defined(Y)
                #endif
                int main(void) { return 0; }
                """)).isEqualTo(1);
    }

    /** Chave de struct e de inicializador de vetor nao sao corpo de funcao. */
    @Test
    void structEInicializadorNaoContamComoFuncao() {
        assertThat(CiclomaticaDeC.contar("""
                struct Ponto { int x; int y; };
                int tabela[] = {1, 2, 3};
                int main(void) { return 0; }
                """).funcoes()).isEqualTo(1);
    }

    /** O total esconde uma funcao isoladamente complexa; e o maximo que a literatura reporta. */
    @Test
    void relataAFuncaoMaisRamificada() {
        CiclomaticaDeC.Contagem contagem = CiclomaticaDeC.contar("""
                int simples(void) { return 0; }
                int complexa(int x) { if (x) { for (int i = 0; i < x; i++) { if (i) return i; } } return 0; }
                """);

        assertThat(contagem.funcoes()).isEqualTo(2);
        assertThat(contagem.maiorPorFuncao()).isEqualTo(4);
    }

    /** Codigo vazio ou so texto nao pode explodir nem devolver zero: M minimo e 1. */
    @Test
    void codigoVazioTemComplexidadeUm() {
        assertThat(m("")).isEqualTo(1);
        assertThat(m("   \n  ")).isEqualTo(1);
        assertThat(m("// so um comentario")).isEqualTo(1);
    }

    /** Literal nao fechado nao pode engolir o resto do arquivo e zerar as decisoes seguintes. */
    @Test
    void literalNaoFechadoParaNaQuebraDeLinha() {
        assertThat(m("""
                int f(int x) {
                  printf("sem fechar
                  if (x) return 1;
                  return 0;
                }
                """)).isEqualTo(2);
    }
}
