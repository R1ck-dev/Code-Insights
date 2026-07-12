package com.projeto.codeinsights.application.knowledge.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.knowledge.dto.MudancaMetricaDTO;
import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO;
import com.projeto.codeinsights.application.knowledge.dto.ReanaliseResolucaoDTO.Status;
import com.projeto.codeinsights.application.knowledge.dto.RelatorioReanaliseDTO;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

@ExtendWith(MockitoExtension.class)
class ReanalisarResolucoesUseCaseTest {

    @Mock
    private ResolucaoRepository resolucaoRepository;
    @Mock
    private ReanalisarResolucaoUseCase reanalisarResolucaoUseCase;

    @InjectMocks
    private ReanalisarResolucoesUseCase useCase;

    private static final MudancaMetricaDTO MUDANCA = new MudancaMetricaDTO(
            TipoMetrica.BIG_O_TEMPO, "O(n^2)", "O(n log n)", NivelConfianca.BAIXA, NivelConfianca.ALTA);

    @Test
    void contaReprocessadasPuladasEFalhasEAgregaODiffDasQueMudaram() {
        UUID comMudanca = UUID.randomUUID();
        UUID semMudanca = UUID.randomUUID();
        UUID pulada = UUID.randomUUID();
        UUID falha = UUID.randomUUID();

        when(resolucaoRepository.listarTodosIds())
                .thenReturn(List.of(comMudanca, semMudanca, pulada, falha));
        when(reanalisarResolucaoUseCase.execute(comMudanca))
                .thenReturn(new ReanaliseResolucaoDTO(comMudanca, Status.REPROCESSADA, List.of(MUDANCA)));
        when(reanalisarResolucaoUseCase.execute(semMudanca))
                .thenReturn(new ReanaliseResolucaoDTO(semMudanca, Status.REPROCESSADA, List.of()));
        when(reanalisarResolucaoUseCase.execute(pulada))
                .thenReturn(new ReanaliseResolucaoDTO(pulada, Status.PULADA, List.of()));
        when(reanalisarResolucaoUseCase.execute(falha))
                .thenReturn(new ReanaliseResolucaoDTO(falha, Status.FALHOU, List.of()));

        RelatorioReanaliseDTO relatorio = useCase.execute(null);

        assertThat(relatorio.total()).isEqualTo(4);
        assertThat(relatorio.reprocessadas()).isEqualTo(2);
        assertThat(relatorio.puladas()).isEqualTo(1);
        assertThat(relatorio.falhas()).isEqualTo(1);
        assertThat(relatorio.comMudanca()).isEqualTo(1);
        assertThat(relatorio.alteracoes()).singleElement().satisfies(alterada -> {
            assertThat(alterada.resolucaoId()).isEqualTo(comMudanca);
            assertThat(alterada.mudancas()).containsExactly(MUDANCA);
        });
    }

    /** Uma resolucao problematica entra no relatorio como falha; a passada segue nas demais. */
    @Test
    void isolaAFalhaDeUmaResolucaoSemAbortarOLote() {
        UUID quebrada = UUID.randomUUID();
        UUID seguinte = UUID.randomUUID();

        when(resolucaoRepository.listarTodosIds()).thenReturn(List.of(quebrada, seguinte));
        when(reanalisarResolucaoUseCase.execute(quebrada))
                .thenThrow(new NegocioException("motor quebrou"));
        when(reanalisarResolucaoUseCase.execute(seguinte))
                .thenReturn(new ReanaliseResolucaoDTO(seguinte, Status.REPROCESSADA, List.of()));

        RelatorioReanaliseDTO relatorio = useCase.execute(null);

        assertThat(relatorio.total()).isEqualTo(2);
        assertThat(relatorio.falhas()).isEqualTo(1);
        assertThat(relatorio.reprocessadas()).isEqualTo(1);
        verify(reanalisarResolucaoUseCase).execute(seguinte);
    }

    @Test
    void restringeAoAutorQuandoInformado() {
        UUID autorId = UUID.randomUUID();
        UUID resolucaoId = UUID.randomUUID();

        when(resolucaoRepository.listarIdsPorAutor(autorId)).thenReturn(List.of(resolucaoId));
        when(reanalisarResolucaoUseCase.execute(resolucaoId))
                .thenReturn(new ReanaliseResolucaoDTO(resolucaoId, Status.REPROCESSADA, List.of()));

        RelatorioReanaliseDTO relatorio = useCase.execute(autorId);

        assertThat(relatorio.total()).isEqualTo(1);
        verify(resolucaoRepository, never()).listarTodosIds();
    }
}
