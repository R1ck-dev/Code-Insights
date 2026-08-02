package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
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
import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

/**
 * O CSV e o artefato que sai da plataforma e vira analise. Um campo mal escapado nao quebra nada
 * visivelmente: ele desloca colunas e produz um numero errado no artigo.
 */
@ExtendWith(MockitoExtension.class)
class ExportarCoorteCsvUseCaseTest {

    @Mock
    private ListarCoorteUseCase listarCoorteUseCase;
    @Mock
    private TermoDeConsentimentoPort termoDeConsentimentoPort;

    @InjectMocks
    private ExportarCoorteCsvUseCase useCase;

    private void coorte(ResolucaoDaCoorteDTO... linhas) {
        when(listarCoorteUseCase.execute()).thenReturn(List.of(linhas));
        lenient().when(termoDeConsentimentoPort.vigente())
                .thenReturn(new TermoDeConsentimento("v1", "Termo", "texto", true));
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
     * Titulo de desafio e texto livre escrito por um aluno, e a planilha do pesquisador le
     * {@code =}, {@code +}, {@code -} e {@code @} no inicio da celula como formula: sem o apostrofo
     * a frente, um titulo {@code =1+1} chega ao artigo como {@code 2}, e um {@code #NAME?} ocupa o
     * lugar do dado sem nenhum aviso. Aspar nao resolveria — o parser da planilha remove as aspas
     * antes de decidir se a celula e formula.
     */
    @Test
    void tituloIniciadoPorSinalDeFormulaEhNeutralizado() {
        for (String titulo : List.of("=1+1", "+SOMA(A1)", "-Fibonacci", "@ARQUIVO")) {
            coorte(linha(titulo));

            assertThat(useCase.execute().conteudo())
                    .as("titulo %s", titulo)
                    .contains(",'" + titulo + ",");
        }
    }

    /** A neutralizacao so vale para o primeiro caractere: titulo comum nao pode ganhar apostrofo. */
    @Test
    void tituloComumAtravessaIntacto() {
        coorte(linha("Two Sum"));

        assertThat(useCase.execute().conteudo()).contains(",Two Sum,");
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

    /**
     * Data para nao sobrescrever o export anterior; versao do termo porque o CSV nao tem onde
     * carregar metadado, e o nome do arquivo e a unica coisa que sobrevive ao download e ao anexo de
     * e-mail. Um arquivo com "v0-rascunho" no nome nao consegue ser confundido com dado de pesquisa.
     */
    @Test
    void oNomeDoArquivoCarregaAVersaoDoTermoEADataDaExportacao() {
        coorte(linha("Two Sum"));

        ArquivoExportadoDTO arquivo = useCase.execute();

        assertThat(arquivo.nomeDoArquivo())
                .matches("codeinsights-coorte-v1-\\d{4}-\\d{2}-\\d{2}\\.csv");
    }

    @Test
    void coorteVaziaProduzArquivoSoComCabecalho() {
        coorte();

        assertThat(linhasDe(useCase.execute().conteudo())).hasSize(1);
    }
}
