package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.pesquisa.dto.ArquivoExportadoDTO;
import com.projeto.codeinsights.application.pesquisa.dto.ResolucaoDaCoorteDTO;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * O CSV e o artefato que sai da plataforma e vira analise. Um campo mal escapado nao quebra nada
 * visivelmente: ele desloca colunas e produz um numero errado no artigo.
 */
@ExtendWith(MockitoExtension.class)
class ExportarCoorteCsvUseCaseTest {

    @Mock
    private ListarCoorteUseCase listarCoorteUseCase;

    @InjectMocks
    private ExportarCoorteCsvUseCase useCase;

    private void coorte(ResolucaoDaCoorteDTO... linhas) {
        when(listarCoorteUseCase.execute()).thenReturn(List.of(linhas));
    }

    private ResolucaoDaCoorteDTO linha(String desafio) {
        return new ResolucaoDaCoorteDTO(UUID.randomUUID(), "A-3F9C21", UUID.randomUUID(), desafio,
                LinguagemProgramacao.JAVA, 4, true,
                "O(n^2)", 4, NivelConfianca.ALTA,
                "O(1)", 0, NivelConfianca.MEDIA,
                7, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private ResolucaoDaCoorteDTO semMetrica() {
        return new ResolucaoDaCoorteDTO(UUID.randomUUID(), "A-7B21EE", UUID.randomUUID(), "Fibonacci",
                LinguagemProgramacao.PYTHON, 2, true,
                null, null, null, null, null, null, null,
                OffsetDateTime.parse("2026-08-02T10:00:00Z"));
    }

    private List<String> linhasDe(String csv) {
        return List.of(csv.split("\r\n"));
    }

    @Test
    void oCabecalhoVemPrimeiroEEmSnakeCase() {
        coorte(linha("Two Sum"));

        String csv = useCase.execute().conteudo();

        assertThat(linhasDe(csv).get(0))
                .isEqualTo("\uFEFFpseudonimo,resolucao_id,desafio_id,desafio,linguagem,autonomia_ia,"
                        + "analisada,tempo_rotulo,tempo_ordem,confianca_tempo,espaco_rotulo,"
                        + "espaco_ordem,confianca_espaco,ciclomatica,submetida_em");
    }

    /** Sem BOM o Excel exibe titulo acentuado como lixo, e o defeito passa despercebido. */
    @Test
    void oArquivoComecaComBomParaOExcelReconhecerUtf8() {
        coorte(linha("Two Sum"));

        assertThat(useCase.execute().conteudo()).startsWith("\uFEFF");
    }

    /** Um titulo com virgula deslocaria todas as colunas seguintes daquela linha. */
    @Test
    void titulosComVirgulaAspasEQuebraDeLinhaSaoEscapados() {
        coorte(linha("Soma, produto e \"resto\"\nsegunda linha"));

        String csv = useCase.execute().conteudo();

        assertThat(csv).contains("\"Soma, produto e \"\"resto\"\"\nsegunda linha\"");
    }

    /**
     * Celula vazia, e nao {@code 0} nem {@code null}: e o que permite a analise separar "sem
     * metrica" de {@code O(1)}, cuja ordem e zero.
     */
    @Test
    void metricaAusenteViraCelulaVaziaENaoZero() {
        coorte(semMetrica());

        String linhaDeDados = linhasDe(useCase.execute().conteudo()).get(1);

        assertThat(linhaDeDados).endsWith("PYTHON,2,true,,,,,,,,2026-08-02T10:00Z");
    }

    @Test
    void produzUmaLinhaPorResolucaoAlemDoCabecalho() {
        coorte(linha("Two Sum"), linha("Fibonacci"), semMetrica());

        assertThat(linhasDe(useCase.execute().conteudo())).hasSize(4);
    }

    @Test
    void oNomeDoArquivoCarregaADataParaNaoSobrescreverExportAnterior() {
        coorte(linha("Two Sum"));

        ArquivoExportadoDTO arquivo = useCase.execute();

        assertThat(arquivo.nomeDoArquivo()).matches("codeinsights-coorte-\\d{4}-\\d{2}-\\d{2}\\.csv");
    }

    @Test
    void coorteVaziaProduzArquivoSoComCabecalho() {
        coorte();

        assertThat(linhasDe(useCase.execute().conteudo())).hasSize(1);
    }
}
