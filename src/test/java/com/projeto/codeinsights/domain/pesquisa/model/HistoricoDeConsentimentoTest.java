package com.projeto.codeinsights.domain.pesquisa.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.pesquisa.enums.DecisaoDeConsentimento;

/**
 * A regra que decide quem entra na pesquisa. Um erro aqui nao quebra nada visivelmente: ele inclui
 * na analise alguem que pediu para sair, ou exclui alguem que autorizou — e nos dois casos so seria
 * descoberto por auditoria.
 */
class HistoricoDeConsentimentoTest {

    private static final String V1 = "v1";
    private static final String V2 = "v2";

    private final UUID ana = UUID.randomUUID();
    private final UUID bruno = UUID.randomUUID();

    private ConsentimentoDePesquisa decisao(UUID participante, String versao,
            DecisaoDeConsentimento tipo, String instante) {
        return new ConsentimentoDePesquisa(UUID.randomUUID(), participante, versao, tipo,
                OffsetDateTime.parse(instante));
    }

    @Test
    void semDecisaoNenhumaNinguemAutoriza() {
        HistoricoDeConsentimento historico = new HistoricoDeConsentimento(List.of(), V1);

        assertThat(historico.participantesQueAutorizam()).isEmpty();
        assertThat(historico.vigenteDe(ana)).isEmpty();
    }

    /** Revogar e uma RECUSA registrada depois de um ACEITE: o aceite continua no log, mas nao vale. */
    @Test
    void aRecusaPosteriorRevogaOAceiteAnterior() {
        HistoricoDeConsentimento historico = new HistoricoDeConsentimento(List.of(
                decisao(ana, V1, DecisaoDeConsentimento.ACEITE, "2026-03-12T10:00:00Z"),
                decisao(ana, V1, DecisaoDeConsentimento.RECUSA, "2026-04-20T10:00:00Z")), V1);

        assertThat(historico.participantesQueAutorizam()).isEmpty();
        assertThat(historico.participantesQueRecusam()).containsExactly(ana);
    }

    /** E o caminho de volta tambem: quem recusou pode aceitar depois, sem tratamento especial. */
    @Test
    void oAceitePosteriorSuperaARecusaAnterior() {
        HistoricoDeConsentimento historico = new HistoricoDeConsentimento(List.of(
                decisao(ana, V1, DecisaoDeConsentimento.RECUSA, "2026-03-12T10:00:00Z"),
                decisao(ana, V1, DecisaoDeConsentimento.ACEITE, "2026-04-20T10:00:00Z")), V1);

        assertThat(historico.participantesQueAutorizam()).containsExactly(ana);
    }

    /**
     * A ordem em que as linhas chegam nao pode decidir nada: hoje elas vem ordenadas do banco, e uma
     * mudanca de indice ou de plano de execucao inverteria isso sem aviso.
     */
    @Test
    void aOrdemDeChegadaDasLinhasNaoAlteraOResultado() {
        List<ConsentimentoDePesquisa> emOrdem = List.of(
                decisao(ana, V1, DecisaoDeConsentimento.ACEITE, "2026-03-12T10:00:00Z"),
                decisao(ana, V1, DecisaoDeConsentimento.RECUSA, "2026-04-20T10:00:00Z"));

        assertThat(new HistoricoDeConsentimento(emOrdem.reversed(), V1).participantesQueAutorizam())
                .isEqualTo(new HistoricoDeConsentimento(emOrdem, V1).participantesQueAutorizam())
                .isEmpty();
    }

    /**
     * Consentir com a v1 nao consente com a v2 — se consentisse, mudar o texto aprovado passaria a
     * valer retroativamente sobre quem leu outra coisa.
     */
    @Test
    void oConsentimentoNaoAtravessaVersoesDoTermo() {
        List<ConsentimentoDePesquisa> decisoes = List.of(
                decisao(ana, V1, DecisaoDeConsentimento.ACEITE, "2026-03-12T10:00:00Z"));

        assertThat(new HistoricoDeConsentimento(decisoes, V1).participantesQueAutorizam())
                .containsExactly(ana);
        assertThat(new HistoricoDeConsentimento(decisoes, V2).participantesQueAutorizam())
                .isEmpty();
    }

    /** Empate exato de instante fica com a recusa: na duvida sobre consentimento, fica de fora. */
    @Test
    void empateNoMesmoInstanteResolveAFavorDaRecusa() {
        HistoricoDeConsentimento historico = new HistoricoDeConsentimento(List.of(
                decisao(ana, V1, DecisaoDeConsentimento.ACEITE, "2026-03-12T10:00:00Z"),
                decisao(ana, V1, DecisaoDeConsentimento.RECUSA, "2026-03-12T10:00:00Z")), V1);

        assertThat(historico.participantesQueAutorizam()).isEmpty();
    }

    @Test
    void cadaParticipanteTemSuaPropriaDecisaoVigente() {
        HistoricoDeConsentimento historico = new HistoricoDeConsentimento(List.of(
                decisao(ana, V1, DecisaoDeConsentimento.ACEITE, "2026-03-12T10:00:00Z"),
                decisao(bruno, V1, DecisaoDeConsentimento.RECUSA, "2026-03-13T10:00:00Z")), V1);

        assertThat(historico.participantesQueAutorizam()).containsExactly(ana);
        assertThat(historico.participantesQueRecusam()).containsExactly(bruno);
        assertThat(historico.vigenteDe(bruno)).get()
                .extracting(ConsentimentoDePesquisa::getDecisao)
                .isEqualTo(DecisaoDeConsentimento.RECUSA);
    }
}
