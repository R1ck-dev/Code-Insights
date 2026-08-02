package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.pesquisa.dto.MeuConsentimentoDTO;
import com.projeto.codeinsights.domain.pesquisa.enums.DecisaoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.model.ConsentimentoDePesquisa;
import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.ConsentimentoRepository;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

@ExtendWith(MockitoExtension.class)
class RegistrarDecisaoDeConsentimentoUseCaseTest {

    private static final String VERSAO = "v1";

    @Mock
    private ConsentimentoRepository consentimentoRepository;
    @Mock
    private TermoDeConsentimentoPort termoDeConsentimentoPort;

    private ObterMeuConsentimentoUseCase obterMeuConsentimentoUseCase;
    private RegistrarDecisaoDeConsentimentoUseCase useCase;

    private final UUID ana = UUID.randomUUID();

    /**
     * O caso de uso de leitura entra real, e nao mockado: a regra "a decisao vigente e a mais
     * recente" e o que decide se este aqui grava ou nao, e substitui-la por um stub testaria o stub.
     */
    @BeforeEach
    void montar() {
        obterMeuConsentimentoUseCase =
                new ObterMeuConsentimentoUseCase(consentimentoRepository, termoDeConsentimentoPort);
        useCase = new RegistrarDecisaoDeConsentimentoUseCase(
                consentimentoRepository, termoDeConsentimentoPort, obterMeuConsentimentoUseCase);

        lenient().when(termoDeConsentimentoPort.vigente())
                .thenReturn(new TermoDeConsentimento(VERSAO, "Termo", "texto", false));
    }

    private void historico(ConsentimentoDePesquisa... decisoes) {
        when(consentimentoRepository.historicoDoParticipante(ana)).thenReturn(List.of(decisoes));
    }

    private ConsentimentoDePesquisa decisao(DecisaoDeConsentimento tipo, String instante) {
        return new ConsentimentoDePesquisa(UUID.randomUUID(), ana, VERSAO, tipo,
                OffsetDateTime.parse(instante));
    }

    @Test
    void gravaOAceiteDeQuemAindaNaoTinhaRespondido() {
        historico();

        MeuConsentimentoDTO dto = useCase.execute(ana, true);

        ArgumentCaptor<ConsentimentoDePesquisa> captor =
                ArgumentCaptor.forClass(ConsentimentoDePesquisa.class);
        verify(consentimentoRepository).registrar(captor.capture());
        assertThat(captor.getValue().getDecisao()).isEqualTo(DecisaoDeConsentimento.ACEITE);
        assertThat(captor.getValue().getVersaoDoTermo()).isEqualTo(VERSAO);
        assertThat(captor.getValue().getParticipanteId()).isEqualTo(ana);
        assertThat(dto.versao()).isEqualTo(VERSAO);
    }

    /** Retirar o consentimento e a mesma operacao, com o outro valor — nao um caminho a parte. */
    @Test
    void aRevogacaoEhUmaLinhaNovaENaoUmaAlteracaoDaAnterior() {
        historico(decisao(DecisaoDeConsentimento.ACEITE, "2026-03-12T10:00:00Z"));

        useCase.execute(ana, false);

        ArgumentCaptor<ConsentimentoDePesquisa> captor =
                ArgumentCaptor.forClass(ConsentimentoDePesquisa.class);
        verify(consentimentoRepository).registrar(captor.capture());
        assertThat(captor.getValue().getDecisao()).isEqualTo(DecisaoDeConsentimento.RECUSA);
    }

    /**
     * Um F5 na tela ou um clique duplo nao sao mudanca de vontade. Gravar linhas identicas encheria
     * a trilha de auditoria de ruido justamente onde ela precisa ser legivel.
     */
    @Test
    void repetirADecisaoVigenteNaoGravaNada() {
        historico(decisao(DecisaoDeConsentimento.ACEITE, "2026-03-12T10:00:00Z"));

        useCase.execute(ana, true);

        verify(consentimentoRepository, never()).registrar(any());
    }

    /** O rascunho nao autoriza pesquisa, e quem aceita precisa ver isso na propria resposta. */
    @Test
    void oTermoNaoAprovadoDevolvePodeSerUsadoEmPesquisaFalso() {
        historico();

        assertThat(useCase.execute(ana, true).podeSerUsadoEmPesquisa()).isFalse();
    }
}
