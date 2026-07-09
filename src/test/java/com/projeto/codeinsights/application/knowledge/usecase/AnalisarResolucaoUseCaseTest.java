package com.projeto.codeinsights.application.knowledge.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class AnalisarResolucaoUseCaseTest {

    @Mock
    private ResolucaoRepository resolucaoRepository;
    @Mock
    private ResultadoMetricaRepository resultadoMetricaRepository;
    @Mock
    private AnalisadorMetricas analisadorMetricas;

    @InjectMocks
    private AnalisarResolucaoUseCase useCase;

    private Resolucao resolucaoNaoAnalisada(UUID id) {
        return new Resolucao(id, UUID.randomUUID(), UUID.randomUUID(), "class A {}",
                LinguagemProgramacao.JAVA, 3, null, Visibilidade.PRIVADO, false, OffsetDateTime.now());
    }

    @Test
    void removeResultadosAntigosPersisteNovosEMarcaComoAnalisada() {
        UUID id = UUID.randomUUID();
        Resolucao resolucao = resolucaoNaoAnalisada(id);
        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.of(resolucao));
        when(analisadorMetricas.analisar(resolucao)).thenReturn(List.of(
                new ResultadoMetrica(null, id, TipoMetrica.COMPLEXIDADE_CICLOMATICA, 1, "1", null,
                        NivelConfianca.ALTA)));

        useCase.execute(id);

        verify(resultadoMetricaRepository).removerPorResolucao(id);
        verify(resultadoMetricaRepository).salvarTodos(any());

        ArgumentCaptor<Resolucao> capturada = ArgumentCaptor.forClass(Resolucao.class);
        verify(resolucaoRepository).salvar(capturada.capture());
        assertThat(capturada.getValue().isAnalisada()).isTrue();
    }

    @Test
    void naoPersisteQuandoNaoHaMetricasMasAindaMarcaComoAnalisada() {
        UUID id = UUID.randomUUID();
        Resolucao resolucao = resolucaoNaoAnalisada(id);
        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.of(resolucao));
        when(analisadorMetricas.analisar(resolucao)).thenReturn(List.of());

        useCase.execute(id);

        verify(resultadoMetricaRepository).removerPorResolucao(id);
        verify(resultadoMetricaRepository, never()).salvarTodos(any());
        verify(resolucaoRepository).salvar(resolucao);
        assertThat(resolucao.isAnalisada()).isTrue();
    }

    @Test
    void falhaQuandoResolucaoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(resolucaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(NegocioException.class);
        verify(resultadoMetricaRepository, never()).removerPorResolucao(any());
    }
}
