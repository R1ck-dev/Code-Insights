package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * Mede o motor contra o gabarito do corpus e grava o resultado em
 * {@code docs/notas-tecnicas/acuracia-do-motor.md}.
 * <p>
 * E um teste, e nao um utilitario avulso, para que o relatorio se regenere a cada
 * {@code ./mvnw test} — um numero de acuracia desatualizado num documento de pesquisa e pior
 * que numero nenhum. Ele <b>nao</b> reprova por acuracia baixa: divergir do gabarito e o dado
 * que estamos coletando. O que ele exige e que toda medicao tenha acontecido.
 */
class RelatorioDeAcuraciaTest {

    private static final Path DESTINO = Path.of("docs", "notas-tecnicas", "acuracia-do-motor.md");

    private final BigOTempoAnalisador tempo = new BigOTempoAnalisador();
    private final EspacoAnalisador espaco = new EspacoAnalisador();

    @Test
    void mediaOCorpusEGravaORelatorio() throws IOException {
        List<Medida> medidas = CorpusDeAlgoritmos.casos().stream().map(this::medir).toList();

        Files.createDirectories(DESTINO.getParent());
        Files.writeString(DESTINO, RelatorioDeAcuracia.renderizar(medidas, LocalDate.now()),
                StandardCharsets.UTF_8);

        System.out.println(RelatorioDeAcuracia.resumoDeUmaLinha(medidas));

        assertThat(medidas)
                .as("todo caso do corpus precisa ter sido analisado")
                .isNotEmpty()
                .allSatisfy(medida -> {
                    assertThat(medida.tempoObtido()).isNotBlank();
                    assertThat(medida.espacoObtido()).isNotBlank();
                    assertThat(medida.confiancaTempo()).isNotNull();
                });
        assertThat(DESTINO).exists();
    }

    private Medida medir(CasoDeCorpus caso) {
        CompilationUnit unidade = AnalisadorTestSupport.parse(caso.codigo());
        MetricaCalculada porTempo = tempo.analisar(unidade);
        MetricaCalculada porEspaco = espaco.analisar(unidade);
        return new Medida(caso, porTempo.rotulo(), porTempo.confianca(),
                porEspaco.rotulo(), porEspaco.confianca());
    }
}
