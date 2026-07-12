package com.projeto.codeinsights.application.knowledge.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.knowledge.dto.MudancaMetricaDTO;
import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO;
import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO.Status;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.AnalisadorMetricas;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResultadoMetricaRepository;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

@ExtendWith(MockitoExtension.class)
class ReanalisarResolucaoUseCaseTest {

    private static final OffsetDateTime SUBMETIDA_EM = OffsetDateTime.parse("2026-03-01T10:15:00-03:00");
    private static final int AUTONOMIA_DECLARADA = 4;
    private static final String APOIO_IA_DECLARADO = "Usei IA apenas para revisar a solucao.";

    @Mock
    private ResolucaoRepository resolucaoRepository;
    @Mock
    private ResultadoMetricaRepository resultadoMetricaRepository;
    @Mock
    private AnalisadorMetricas analisadorMetricas;

    @InjectMocks
    private ReanalisarResolucaoUseCase useCase;

    private Resolucao gravada(UUID id, LinguagemProgramacao linguagem, boolean analisada) {
        return new Resolucao(id, UUID.randomUUID(), UUID.randomUUID(), "class A {}", linguagem,
                AUTONOMIA_DECLARADA, APOIO_IA_DECLARADO, Visibilidade.PUBLICO, analisada, SUBMETIDA_EM);
    }

    private ResultadoMetrica ciclomatica(UUID resolucaoId, NivelConfianca confianca) {
        return new ResultadoMetrica(null, resolucaoId, TipoMetrica.COMPLEXIDADE_CICLOMATICA, 7, "7",
                null, confianca);
    }

    /**
     * O caso real: a ciclomatica e uma contagem exata, mas as linhas anteriores a V7 herdaram
     * confianca BAIXA do DEFAULT da coluna. Reprocessar tem de corrigir isso e reportar a mudanca —
     * ainda que o rotulo ("7") nao mude.
     */
    @Test
    void substituiMetricasAntigasSemDuplicarEReportaMudancaDeConfianca() {
        UUID id = UUID.randomUUID();
        Resolucao resolucao = gravada(id, LinguagemProgramacao.JAVA, true);
        List<ResultadoMetrica> novos = List.of(ciclomatica(id, NivelConfianca.ALTA));

        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.of(resolucao));
        when(analisadorMetricas.suporta(LinguagemProgramacao.JAVA)).thenReturn(true);
        when(analisadorMetricas.analisar(resolucao)).thenReturn(novos);
        when(resultadoMetricaRepository.listarPorResolucao(id))
                .thenReturn(List.of(ciclomatica(id, NivelConfianca.BAIXA)));

        ReanaliseResolucaoDTO resultado = useCase.execute(id);

        assertThat(resultado.status()).isEqualTo(Status.REPROCESSADA);
        assertThat(resultado.mudancas()).containsExactly(new MudancaMetricaDTO(
                TipoMetrica.COMPLEXIDADE_CICLOMATICA, "7", "7", NivelConfianca.BAIXA, NivelConfianca.ALTA));

        // Apaga antes de gravar (a UNIQUE(resolucao_id, tipo) nao tolera a linha antiga e a nova juntas)
        // e grava exatamente uma vez: reprocessar substitui, nao duplica.
        InOrder ordem = inOrder(resultadoMetricaRepository);
        ordem.verify(resultadoMetricaRepository).removerPorResolucao(id);
        ordem.verify(resultadoMetricaRepository).salvarTodos(novos);
    }

    @Test
    void pulaResolucaoDeLinguagemSemAnalisadorSemApagarNadaNemMarcarComoAnalisada() {
        UUID id = UUID.randomUUID();
        Resolucao resolucao = gravada(id, LinguagemProgramacao.PYTHON, false);

        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.of(resolucao));
        when(analisadorMetricas.suporta(LinguagemProgramacao.PYTHON)).thenReturn(false);

        ReanaliseResolucaoDTO resultado = useCase.execute(id);

        assertThat(resultado.status()).isEqualTo(Status.PULADA);
        assertThat(resultado.mudancas()).isEmpty();
        assertThat(resolucao.isAnalisada()).isFalse();
        verifyNoInteractions(resultadoMetricaRepository);
        verify(analisadorMetricas, never()).analisar(any());
        verify(resolucaoRepository, never()).salvar(any());
    }

    @Test
    void preservaAsMetricasGravadasQuandoOCodigoNaoParseia() {
        UUID id = UUID.randomUUID();
        Resolucao resolucao = gravada(id, LinguagemProgramacao.JAVA, false);

        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.of(resolucao));
        when(analisadorMetricas.suporta(LinguagemProgramacao.JAVA)).thenReturn(true);
        when(analisadorMetricas.analisar(resolucao)).thenReturn(List.of());

        ReanaliseResolucaoDTO resultado = useCase.execute(id);

        assertThat(resultado.status()).isEqualTo(Status.FALHOU);
        assertThat(resolucao.isAnalisada()).isFalse();
        verifyNoInteractions(resultadoMetricaRepository);
        verify(resolucaoRepository, never()).salvar(any());
    }

    /** O motor so deriva metricas do codigo: autodeclarado do aluno e eixo do tempo da pesquisa sao intocaveis. */
    @Test
    void naoTocaEmAutonomiaApoioIaSubmissaoNemVisibilidade() {
        UUID id = UUID.randomUUID();
        Resolucao resolucao = gravada(id, LinguagemProgramacao.JAVA, false);

        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.of(resolucao));
        when(analisadorMetricas.suporta(LinguagemProgramacao.JAVA)).thenReturn(true);
        when(analisadorMetricas.analisar(resolucao)).thenReturn(List.of(ciclomatica(id, NivelConfianca.ALTA)));
        when(resultadoMetricaRepository.listarPorResolucao(id)).thenReturn(List.of());

        useCase.execute(id);

        ArgumentCaptor<Resolucao> salva = ArgumentCaptor.forClass(Resolucao.class);
        verify(resolucaoRepository).salvar(salva.capture());
        assertThat(salva.getValue().getIndiceAutonomiaIA()).isEqualTo(AUTONOMIA_DECLARADA);
        assertThat(salva.getValue().getDescricaoApoioIA()).isEqualTo(APOIO_IA_DECLARADO);
        assertThat(salva.getValue().getSubmetidaEm()).isEqualTo(SUBMETIDA_EM);
        assertThat(salva.getValue().getVisibilidade()).isEqualTo(Visibilidade.PUBLICO);
        assertThat(salva.getValue().isAnalisada()).isTrue();
    }

    @Test
    void naoReportaMudancaQuandoOMotorConfirmaOQueJaEstavaGravado() {
        UUID id = UUID.randomUUID();
        Resolucao resolucao = gravada(id, LinguagemProgramacao.JAVA, true);
        List<ResultadoMetrica> novos = List.of(ciclomatica(id, NivelConfianca.ALTA));

        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.of(resolucao));
        when(analisadorMetricas.suporta(LinguagemProgramacao.JAVA)).thenReturn(true);
        when(analisadorMetricas.analisar(resolucao)).thenReturn(novos);
        when(resultadoMetricaRepository.listarPorResolucao(id))
                .thenReturn(List.of(ciclomatica(id, NivelConfianca.ALTA)));

        ReanaliseResolucaoDTO resultado = useCase.execute(id);

        assertThat(resultado.status()).isEqualTo(Status.REPROCESSADA);
        assertThat(resultado.mudancas()).isEmpty();
        verify(resultadoMetricaRepository).salvarTodos(novos);
    }

    @Test
    void falhaQuandoResolucaoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(NegocioException.class);
        verifyNoInteractions(resultadoMetricaRepository);
    }
}
