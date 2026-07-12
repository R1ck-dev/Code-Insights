package com.projeto.codeinsights.application.knowledge.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.knowledge.dto.ResolucaoResumoDTO;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

@ExtendWith(MockitoExtension.class)
class ListarResolucoesDoDesafioUseCaseTest {

    @Mock
    private ResolucaoRepository resolucaoRepository;

    @Mock
    private DesafioRepository desafioRepository;

    @Mock
    private ResultadoMetricaRepository resultadoMetricaRepository;

    @InjectMocks
    private ListarResolucoesDoDesafioUseCase useCase;

    private final UUID autorId = UUID.randomUUID();
    private final UUID desafioId = UUID.randomUUID();

    private Desafio desafioDoAutor() {
        return new Desafio(desafioId, autorId, "Two Sum", null, null, null, null);
    }

    private Resolucao resolucao(LinguagemProgramacao linguagem, int autonomia) {
        return new Resolucao(null, autorId, desafioId, "class A {}", linguagem, autonomia, null);
    }

    private ResultadoMetrica tempo(UUID resolucaoId, int valor, String rotulo, NivelConfianca confianca) {
        return new ResultadoMetrica(null, resolucaoId, TipoMetrica.BIG_O_TEMPO, valor, rotulo, null, confianca);
    }

    private void darPagina(List<Resolucao> itens) {
        when(desafioRepository.buscarPorId(desafioId)).thenReturn(Optional.of(desafioDoAutor()));
        when(resolucaoRepository.listarPorDesafio(eq(desafioId), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(itens, 0, 1, itens.size()));
    }

    @Test
    void expoeBigODeTempoNoResumoQuandoAResolucaoFoiAnalisada() {
        Resolucao resolucao = resolucao(LinguagemProgramacao.JAVA, 4);
        darPagina(List.of(resolucao));
        when(resultadoMetricaRepository.listarPorResolucoesETipo(any(), eq(TipoMetrica.BIG_O_TEMPO)))
                .thenReturn(List.of(tempo(resolucao.getId(), 3, "O(n log n)", NivelConfianca.ALTA)));

        Pagina<ResolucaoResumoDTO> pagina = useCase.execute(desafioId, autorId, 0, 20);

        assertThat(pagina.itens()).singleElement().satisfies(r -> {
            assertThat(r.id()).isEqualTo(resolucao.getId());
            assertThat(r.tempoRotulo()).isEqualTo("O(n log n)");
            assertThat(r.tempoOrdem()).isEqualTo(3);
            assertThat(r.confiancaTempo()).isEqualTo(NivelConfianca.ALTA);
        });
    }

    /** O(1) e uma complexidade legitima: a ordem 0 tem de sobreviver ate o DTO, e nao virar nulo. */
    @Test
    void preservaOrdemZeroComoODeUmEnaoComoAusenciaDeDado() {
        Resolucao resolucao = resolucao(LinguagemProgramacao.JAVA, 5);
        darPagina(List.of(resolucao));
        when(resultadoMetricaRepository.listarPorResolucoesETipo(any(), eq(TipoMetrica.BIG_O_TEMPO)))
                .thenReturn(List.of(tempo(resolucao.getId(), 0, "O(1)", NivelConfianca.ALTA)));

        ResolucaoResumoDTO resumo = useCase.execute(desafioId, autorId, 0, 20).itens().getFirst();

        assertThat(resumo.tempoOrdem()).isZero();
        assertThat(resumo.tempoRotulo()).isEqualTo("O(1)");
    }

    /** -1 = o motor rodou e NAO classificou. E diferente de nulo (nao ha dado) — o front depende disso. */
    @Test
    void distingueDesconhecidoMenosUmDeAusenciaDeMetricaNula() {
        Resolucao naoClassificada = resolucao(LinguagemProgramacao.JAVA, 3);
        Resolucao semAnalisador = resolucao(LinguagemProgramacao.PYTHON, 2);
        darPagina(List.of(naoClassificada, semAnalisador));
        when(resultadoMetricaRepository.listarPorResolucoesETipo(any(), eq(TipoMetrica.BIG_O_TEMPO)))
                .thenReturn(List.of(tempo(naoClassificada.getId(), -1, "?", NivelConfianca.BAIXA)));

        List<ResolucaoResumoDTO> itens = useCase.execute(desafioId, autorId, 0, 20).itens();

        assertThat(itens.get(0).tempoOrdem()).isEqualTo(-1);
        assertThat(itens.get(0).tempoRotulo()).isEqualTo("?");
        assertThat(itens.get(0).confiancaTempo()).isEqualTo(NivelConfianca.BAIXA);

        // Sem analisador (nao-Java): ausencia de dado, nao "-1" e nao zero.
        assertThat(itens.get(1).tempoOrdem()).isNull();
        assertThat(itens.get(1).tempoRotulo()).isNull();
        assertThat(itens.get(1).confiancaTempo()).isNull();
    }

    /** Sem N+1: uma unica consulta de metrica para a pagina inteira, com todos os ids de uma vez. */
    @Test
    void buscaAsMetricasDaPaginaInteiraNumaUnicaConsulta() {
        List<Resolucao> itens = List.of(
                resolucao(LinguagemProgramacao.JAVA, 1),
                resolucao(LinguagemProgramacao.JAVA, 3),
                resolucao(LinguagemProgramacao.JAVA, 5));
        darPagina(itens);
        when(resultadoMetricaRepository.listarPorResolucoesETipo(any(), eq(TipoMetrica.BIG_O_TEMPO)))
                .thenReturn(List.of());

        useCase.execute(desafioId, autorId, 0, 20);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> ids = ArgumentCaptor.forClass(List.class);
        verify(resultadoMetricaRepository, times(1))
                .listarPorResolucoesETipo(ids.capture(), eq(TipoMetrica.BIG_O_TEMPO));
        assertThat(ids.getValue())
                .containsExactly(itens.get(0).getId(), itens.get(1).getId(), itens.get(2).getId());
    }

    @Test
    void naoConsultaMetricasQuandoODesafioNaoTemResolucoes() {
        darPagina(List.of());
        when(resultadoMetricaRepository.listarPorResolucoesETipo(any(), eq(TipoMetrica.BIG_O_TEMPO)))
                .thenReturn(List.of());

        assertThat(useCase.execute(desafioId, autorId, 0, 20).itens()).isEmpty();
    }

    @Test
    void negaListagemDeDesafioPrivadoAQuemNaoEODono() {
        when(desafioRepository.buscarPorId(desafioId)).thenReturn(Optional.of(desafioDoAutor()));

        assertThatThrownBy(() -> useCase.execute(desafioId, UUID.randomUUID(), 0, 20))
                .isInstanceOf(NegocioException.class);

        verify(resultadoMetricaRepository, never()).listarPorResolucoesETipo(any(), any());
    }
}
