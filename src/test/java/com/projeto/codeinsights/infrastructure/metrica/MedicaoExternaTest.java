package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.infrastructure.metrica.ExecucaoDoExterno.Resultado;
import com.projeto.codeinsights.infrastructure.metrica.PortaoDaMedicao.Decisao;

/**
 * A medicao unica do corpus externo (Adendos 6 e 7), a unica classe que conhece a pasta real dos
 * fontes e o relatorio real. Num build comum fica <b>ignorada</b>, com a mensagem de por que: so roda
 * com a pasta presente e um pedido explicito no comando ({@code -Dcorpus.externo=medir}, ou
 * {@code remedir} para refazer por defeito do teste ou do relatorio). A decisao esta em
 * {@link PortaoDaMedicao}.
 * <p>
 * Nao reprova por acuracia: divergir da nota e o dado que se coleta. Falha so se a carga, o hash ou a
 * gravacao do bloco falharem. Imprime o bloco do relatorio e uma linha de contagens por linguagem; o
 * codigo de terceiros nunca chega a nenhum objeto que se imprime.
 */
class MedicaoExternaTest {

    private static final Path FONTES = Path.of("docs", "corpus-externo");
    private static final Path RELATORIO = Path.of("docs", "notas-tecnicas", "acuracia-do-motor.md");

    @Test
    void medeOCorpusExternoUmaVezEGravaOBloco() {
        Decisao decisao = PortaoDaMedicao.decidir(FONTES.toString(), Files.isDirectory(FONTES),
                System.getProperty(PortaoDaMedicao.PROPRIEDADE));
        assumeTrue(decisao.executa(), decisao.mensagem());

        Resultado resultado = ExecucaoDoExterno.executar(FONTES, GabaritoExterno.dosResources(),
                ManifestoExterno.dosResources(), RELATORIO, decisao.modo());

        ResumoExterno.porLinguagem(resultado.medidas()).forEach(r -> System.out.println(r.linhaDeContagens()));
        assertThat(resultado.medidas()).as("a carga devolve todos os casos do gabarito").isNotEmpty();
    }
}
