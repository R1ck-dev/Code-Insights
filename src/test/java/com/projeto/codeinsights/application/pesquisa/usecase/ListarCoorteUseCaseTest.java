package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.pesquisa.dto.ResolucaoDaCoorteDTO;
import com.projeto.codeinsights.domain.pesquisa.model.PseudonimoDeAluno;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;

@ExtendWith(MockitoExtension.class)
class ListarCoorteUseCaseTest {

    @Mock
    private CoorteRepository coorteRepository;
    @Mock
    private ObterParticipantesConsentidosUseCase obterParticipantesConsentidosUseCase;

    @InjectMocks
    private ListarCoorteUseCase useCase;

    private final UUID ana = UUID.randomUUID();
    private final UUID bruno = UUID.randomUUID();

    /** Cenario comum: todos os autores das linhas consentiram. */
    private void coorte(ResolucaoDaCoorte... linhas) {
        Set<UUID> consentidos = Arrays.stream(linhas)
                .map(ResolucaoDaCoorte::autorId)
                .collect(Collectors.toSet());
        when(obterParticipantesConsentidosUseCase.execute()).thenReturn(consentidos);
        when(coorteRepository.listarCoorte(consentidos)).thenReturn(List.of(linhas));
    }

    @Test
    void oMesmoAutorRecebeOMesmoPseudonimoEmTodasAsSuasLinhas() {
        coorte(
                CoorteDeTeste.analisada(ana, 4),
                CoorteDeTeste.analisada(ana, 2),
                CoorteDeTeste.analisada(bruno, 5));

        List<ResolucaoDaCoorteDTO> linhas = useCase.execute();

        assertThat(linhas).hasSize(3);
        assertThat(linhas.get(0).pseudonimo())
                .isEqualTo(linhas.get(1).pseudonimo())
                .isEqualTo(PseudonimoDeAluno.de(ana));
        assertThat(linhas.get(2).pseudonimo()).isEqualTo(PseudonimoDeAluno.de(bruno));
    }

    /**
     * O consentimento e resolvido <b>antes</b> da consulta: quem nao autorizou nao e filtrado
     * depois, o id dele nem entra no {@code in} da JPQL. Este teste prende isso verificando o
     * argumento — se alguem trocar por um filtro posterior, os dados de quem recusou passariam a
     * sair do banco, e so um descuido separaria isso de vazarem para a tela.
     */
    @Test
    void consultaSomenteOsParticipantesQueConsentiram() {
        when(obterParticipantesConsentidosUseCase.execute()).thenReturn(Set.of(ana));
        when(coorteRepository.listarCoorte(Set.of(ana)))
                .thenReturn(List.of(CoorteDeTeste.analisada(ana, 4)));

        useCase.execute();

        verify(coorteRepository).listarCoorte(Set.of(ana));
    }

    /**
     * O DTO nao tem componente para o id do autor — este teste prende essa ausencia. Se alguem
     * acrescentar o campo por conveniencia, a identidade passa a sair na listagem e no CSV sem
     * que ninguem note.
     */
    @Test
    void oIdentificadorRealDoAutorNaoSaiNaListagem() {
        coorte(CoorteDeTeste.analisada(ana, 4));

        assertThat(useCase.execute()).singleElement().satisfies(linha ->
                assertThat(linha.toString()).doesNotContain(ana.toString()));
    }

    @Test
    void preservaOsNulosDeMetricaEmResolucaoNaoAnalisada() {
        coorte(CoorteDeTeste.aguardando(ana));

        assertThat(useCase.execute()).singleElement().satisfies(linha -> {
            assertThat(linha.analisada()).isFalse();
            assertThat(linha.tempoOrdem()).isNull();
            assertThat(linha.espacoOrdem()).isNull();
            assertThat(linha.ciclomatica()).isNull();
        });
    }

    /** Ninguem consentiu: a coorte e vazia, e nao "a plataforma inteira". */
    @Test
    void coorteVaziaProduzListaVazia() {
        coorte();

        assertThat(useCase.execute()).isEmpty();
    }
}
