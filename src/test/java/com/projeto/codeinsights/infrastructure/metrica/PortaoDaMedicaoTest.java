package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.infrastructure.metrica.ExecucaoDoExterno.Modo;
import com.projeto.codeinsights.infrastructure.metrica.PortaoDaMedicao.Acao;
import com.projeto.codeinsights.infrastructure.metrica.PortaoDaMedicao.Decisao;

/**
 * O portao da medicao unica, como funcao pura: nenhum caso aqui executa a medicao nem olha o disco.
 * A ordem das checagens e parte do contrato: pasta primeiro, pedido depois.
 */
class PortaoDaMedicaoTest {

    private static final String PASTA = "pasta/de/mentira";

    @Test
    void pastaAusenteIgnoraComAMensagemDaSecao7() {
        Decisao decisao = PortaoDaMedicao.decidir(PASTA, false, null);

        assertThat(decisao.acao()).isEqualTo(Acao.IGNORAR);
        assertThat(decisao.executa()).isFalse();
        assertThat(decisao.mensagem()).startsWith("pasta local " + PASTA + " ausente").contains("secao 7");
    }

    @Test
    void pastaAusenteVenceOPedido() {
        for (String pedido : new String[] {"medir", "remedir", "qualquer"}) {
            Decisao decisao = PortaoDaMedicao.decidir(PASTA, false, pedido);

            assertThat(decisao.acao()).as(pedido).isEqualTo(Acao.IGNORAR);
            assertThat(decisao.mensagem()).as(pedido).startsWith("pasta local " + PASTA + " ausente");
        }
    }

    @Test
    void pastaPresenteSemPedidoIgnoraCitandoAMedicaoUnicaEOAdendo6() {
        Decisao decisao = PortaoDaMedicao.decidir(PASTA, true, null);

        assertThat(decisao.acao()).isEqualTo(Acao.IGNORAR);
        assertThat(decisao.mensagem()).contains("medicao unica").contains("Adendo 6, item 9")
                .contains("-Dcorpus.externo=medir");
    }

    @Test
    void pedidoEmBrancoValeComoSemPedido() {
        for (String pedido : new String[] {"", "   "}) {
            Decisao decisao = PortaoDaMedicao.decidir(PASTA, true, pedido);

            assertThat(decisao.acao()).as("[" + pedido + "]").isEqualTo(Acao.IGNORAR);
            assertThat(decisao.mensagem()).contains("Adendo 6, item 9");
        }
    }

    @Test
    void medirExecutaSemPermissaoDeSobrescrever() {
        Decisao decisao = PortaoDaMedicao.decidir(PASTA, true, "medir");

        assertThat(decisao.acao()).isEqualTo(Acao.MEDIR);
        assertThat(decisao.executa()).isTrue();
        assertThat(decisao.modo()).isEqualTo(Modo.MEDIR);
    }

    @Test
    void remedirExecutaComPermissaoDeSobrescrever() {
        Decisao decisao = PortaoDaMedicao.decidir(PASTA, true, "remedir");

        assertThat(decisao.acao()).isEqualTo(Acao.REMEDIR);
        assertThat(decisao.executa()).isTrue();
        assertThat(decisao.modo()).isEqualTo(Modo.REMEDIR);
    }

    @Test
    void outroValorIgnoraComMensagemQueListaOsValidos() {
        for (String pedido : new String[] {"true", "Medir", "MEDIR", "medir ", "sim", "1"}) {
            Decisao decisao = PortaoDaMedicao.decidir(PASTA, true, pedido);

            assertThat(decisao.acao()).as("[" + pedido + "]").isEqualTo(Acao.IGNORAR);
            assertThat(decisao.mensagem()).as("[" + pedido + "]").contains("nao reconhecido").contains("medir")
                    .contains("remedir");
        }
    }

    @Test
    void decisaoDeIgnorarNaoTemModoDeExecucao() {
        Decisao ignorada = PortaoDaMedicao.decidir(PASTA, false, "medir");

        assertThatThrownBy(ignorada::modo).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aPropriedadeSeChamaCorpusExterno() {
        assertThat(PortaoDaMedicao.PROPRIEDADE).isEqualTo("corpus.externo");
    }
}
