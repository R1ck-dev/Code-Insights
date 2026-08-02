package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.pesquisa.dto.ResolucaoDaCoorteDTO;
import com.projeto.codeinsights.domain.pesquisa.model.PseudonimoDeAluno;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;

@ExtendWith(MockitoExtension.class)
class ListarCoorteUseCaseTest {

    @Mock
    private CoorteRepository coorteRepository;

    @InjectMocks
    private ListarCoorteUseCase useCase;

    private final UUID ana = UUID.randomUUID();
    private final UUID bruno = UUID.randomUUID();

    @Test
    void oMesmoAutorRecebeOMesmoPseudonimoEmTodasAsSuasLinhas() {
        when(coorteRepository.listarCoorte()).thenReturn(List.of(
                CoorteDeTeste.analisada(ana, 4),
                CoorteDeTeste.analisada(ana, 2),
                CoorteDeTeste.analisada(bruno, 5)));

        List<ResolucaoDaCoorteDTO> linhas = useCase.execute();

        assertThat(linhas).hasSize(3);
        assertThat(linhas.get(0).pseudonimo())
                .isEqualTo(linhas.get(1).pseudonimo())
                .isEqualTo(PseudonimoDeAluno.de(ana));
        assertThat(linhas.get(2).pseudonimo()).isEqualTo(PseudonimoDeAluno.de(bruno));
    }

    /**
     * O DTO nao tem componente para o id do autor — este teste prende essa ausencia. Se alguem
     * acrescentar o campo por conveniencia, a identidade passa a sair na listagem e no CSV sem
     * que ninguem note.
     */
    @Test
    void oIdentificadorRealDoAutorNaoSaiNaListagem() {
        when(coorteRepository.listarCoorte()).thenReturn(List.of(CoorteDeTeste.analisada(ana, 4)));

        assertThat(useCase.execute()).singleElement().satisfies(linha ->
                assertThat(linha.toString()).doesNotContain(ana.toString()));
    }

    @Test
    void preservaOsNulosDeMetricaEmResolucaoNaoAnalisada() {
        when(coorteRepository.listarCoorte()).thenReturn(List.of(CoorteDeTeste.aguardando(ana)));

        assertThat(useCase.execute()).singleElement().satisfies(linha -> {
            assertThat(linha.analisada()).isFalse();
            assertThat(linha.tempoOrdem()).isNull();
            assertThat(linha.espacoOrdem()).isNull();
            assertThat(linha.ciclomatica()).isNull();
        });
    }

    @Test
    void coorteVaziaProduzListaVazia() {
        when(coorteRepository.listarCoorte()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}
