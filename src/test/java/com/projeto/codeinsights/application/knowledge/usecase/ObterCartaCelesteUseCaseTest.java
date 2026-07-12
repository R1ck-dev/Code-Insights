package com.projeto.codeinsights.application.knowledge.usecase;

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

import com.projeto.codeinsights.application.knowledge.dto.PontoCartaDTO;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.model.PontoCarta;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

@ExtendWith(MockitoExtension.class)
class ObterCartaCelesteUseCaseTest {

    @Mock
    private ResolucaoRepository resolucaoRepository;

    @InjectMocks
    private ObterCartaCelesteUseCase useCase;

    @Test
    void mapeiaPontoAnalisadoPreservandoRotulosOrdensEConfianca() {
        UUID autorId = UUID.randomUUID();
        UUID resolucaoId = UUID.randomUUID();
        UUID desafioId = UUID.randomUUID();
        OffsetDateTime submetidaEm = OffsetDateTime.now();
        when(resolucaoRepository.listarPontosCartaPorAutor(autorId)).thenReturn(List.of(
                new PontoCarta(resolucaoId, desafioId, "Two Sum", LinguagemProgramacao.JAVA, 4, true,
                        "O(n)", 2, NivelConfianca.ALTA, "O(n)", 2, 3,
                        Visibilidade.PUBLICO, submetidaEm)));

        List<PontoCartaDTO> pontos = useCase.execute(autorId);

        assertThat(pontos).singleElement().satisfies(p -> {
            assertThat(p.resolucaoId()).isEqualTo(resolucaoId);
            assertThat(p.desafioId()).isEqualTo(desafioId);
            assertThat(p.desafioTitulo()).isEqualTo("Two Sum");
            assertThat(p.linguagem()).isEqualTo(LinguagemProgramacao.JAVA);
            assertThat(p.indiceAutonomiaIA()).isEqualTo(4);
            assertThat(p.analisada()).isTrue();
            assertThat(p.tempoRotulo()).isEqualTo("O(n)");
            assertThat(p.tempoOrdem()).isEqualTo(2);
            assertThat(p.confiancaTempo()).isEqualTo(NivelConfianca.ALTA);
            assertThat(p.espacoRotulo()).isEqualTo("O(n)");
            assertThat(p.espacoOrdem()).isEqualTo(2);
            assertThat(p.ciclomatica()).isEqualTo(3);
            assertThat(p.visibilidade()).isEqualTo(Visibilidade.PUBLICO);
            assertThat(p.submetidaEm()).isEqualTo(submetidaEm);
        });
    }

    /** Nao analisada nao vira O(1)/ciclomatica 0: as metricas continuam nulas ate a analise rodar. */
    @Test
    void mantemMetricasNulasQuandoResolucaoAindaNaoFoiAnalisada() {
        UUID autorId = UUID.randomUUID();
        when(resolucaoRepository.listarPontosCartaPorAutor(autorId)).thenReturn(List.of(
                new PontoCarta(UUID.randomUUID(), UUID.randomUUID(), "Recem submetida",
                        LinguagemProgramacao.PYTHON, 2, false,
                        null, null, null, null, null, null,
                        Visibilidade.PRIVADO, OffsetDateTime.now())));

        List<PontoCartaDTO> pontos = useCase.execute(autorId);

        assertThat(pontos).singleElement().satisfies(p -> {
            assertThat(p.analisada()).isFalse();
            assertThat(p.tempoRotulo()).isNull();
            assertThat(p.tempoOrdem()).isNull();
            assertThat(p.confiancaTempo()).isNull();
            assertThat(p.espacoRotulo()).isNull();
            assertThat(p.espacoOrdem()).isNull();
            assertThat(p.ciclomatica()).isNull();
        });
    }

    @Test
    void devolveListaVaziaQuandoAutorNaoTemResolucoes() {
        UUID autorId = UUID.randomUUID();
        when(resolucaoRepository.listarPontosCartaPorAutor(autorId)).thenReturn(List.of());

        assertThat(useCase.execute(autorId)).isEmpty();
    }
}
