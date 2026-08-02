package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.pesquisa.dto.ContagemDTO;
import com.projeto.codeinsights.application.pesquisa.dto.QualidadeDaCoorteDTO;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.port.AnalisadorMetricas;
import com.projeto.codeinsights.domain.pesquisa.enums.DecisaoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.model.ConsentimentoDePesquisa;
import com.projeto.codeinsights.domain.pesquisa.model.HistoricoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

@ExtendWith(MockitoExtension.class)
class ObterQualidadeDaCoorteUseCaseTest {

    private static final String VERSAO = "v1";

    @Mock
    private CoorteRepository coorteRepository;
    @Mock
    private ObterParticipantesConsentidosUseCase obterParticipantesConsentidosUseCase;
    @Mock
    private TermoDeConsentimentoPort termoDeConsentimentoPort;
    @Mock
    private AnalisadorMetricas analisadorMetricas;

    @InjectMocks
    private ObterQualidadeDaCoorteUseCase useCase;

    private final UUID ana = UUID.randomUUID();
    private final UUID bruno = UUID.randomUUID();
    private final UUID carla = UUID.randomUUID();

    private void soJavaEhSuportado() {
        when(analisadorMetricas.suporta(any())).thenAnswer(chamada ->
                chamada.getArgument(0) == LinguagemProgramacao.JAVA);
    }

    /** Cenario comum: todos os autores das linhas consentiram, e ninguem mais submeteu. */
    private void coorte(ResolucaoDaCoorte... resolucoes) {
        Set<UUID> autores = new LinkedHashSet<>(
                Arrays.stream(resolucoes).map(ResolucaoDaCoorte::autorId).toList());
        cenario(autores, Set.of(), autores, resolucoes);
    }

    /**
     * @param aceitaram quem consentiu com a versao vigente
     * @param recusaram quem respondeu "nao"
     * @param submeteram todos os que tem ao menos uma resolucao — o denominador da cobertura
     */
    private void cenario(Set<UUID> aceitaram, Set<UUID> recusaram, Set<UUID> submeteram,
            ResolucaoDaCoorte... resolucoesDaCoorte) {
        when(termoDeConsentimentoPort.vigente())
                .thenReturn(new TermoDeConsentimento(VERSAO, "Termo", "texto", true));
        when(obterParticipantesConsentidosUseCase.historicoVigente())
                .thenReturn(historicoCom(aceitaram, recusaram));
        when(coorteRepository.listarCoorte(aceitaram)).thenReturn(List.of(resolucoesDaCoorte));
        when(coorteRepository.autoresComResolucao()).thenReturn(submeteram);
        when(coorteRepository.totalDeResolucoes()).thenReturn((long) resolucoesDaCoorte.length);
    }

    private HistoricoDeConsentimento historicoCom(Set<UUID> aceitaram, Set<UUID> recusaram) {
        List<ConsentimentoDePesquisa> decisoes = new ArrayList<>();
        aceitaram.forEach(id -> decisoes.add(
                new ConsentimentoDePesquisa(id, VERSAO, DecisaoDeConsentimento.ACEITE)));
        recusaram.forEach(id -> decisoes.add(
                new ConsentimentoDePesquisa(id, VERSAO, DecisaoDeConsentimento.RECUSA)));
        return new HistoricoDeConsentimento(decisoes, VERSAO);
    }

    /**
     * A propriedade que sustenta a tela: os quatro baldes de cobertura somam o total. Se um caso
     * escapar de todos ou entrar em dois, a taxa exibida vira ficcao.
     */
    @Test
    void osQuatroBaldesDeCoberturaParticionamAAmostra() {
        soJavaEhSuportado();
        coorte(
                CoorteDeTeste.analisada(ana, 4),
                CoorteDeTeste.aguardando(ana),
                CoorteDeTeste.falhaDeAnalise(bruno),
                CoorteDeTeste.semAnalisador(bruno, LinguagemProgramacao.PYTHON));

        QualidadeDaCoorteDTO qualidade = useCase.execute();

        assertThat(qualidade.comMetrica()).isEqualTo(1);
        assertThat(qualidade.aguardandoAnalise()).isEqualTo(1);
        assertThat(qualidade.falhaDeAnalise()).isEqualTo(1);
        assertThat(qualidade.semAnalisadorDeLinguagem()).isEqualTo(1);
        assertThat(qualidade.comMetrica() + qualidade.aguardandoAnalise()
                + qualidade.falhaDeAnalise() + qualidade.semAnalisadorDeLinguagem())
                .isEqualTo(qualidade.resolucoes());
    }

    /**
     * Os dois estados sao identicos no banco (analisada, sem metrica) e opostos em significado:
     * um e escopo conhecido, o outro e defeito a investigar. So a porta os separa.
     */
    @Test
    void linguagemSemAnalisadorNaoContaComoFalhaDeAnalise() {
        soJavaEhSuportado();
        coorte(CoorteDeTeste.semAnalisador(ana, LinguagemProgramacao.CPP));

        QualidadeDaCoorteDTO qualidade = useCase.execute();

        assertThat(qualidade.semAnalisadorDeLinguagem()).isEqualTo(1);
        assertThat(qualidade.falhaDeAnalise()).isZero();
    }

    @Test
    void contaParticipantesDistintosEQuemSubmeteuUmaVezSo() {
        soJavaEhSuportado();
        coorte(
                CoorteDeTeste.analisada(ana, 4),
                CoorteDeTeste.analisada(ana, 3),
                CoorteDeTeste.analisada(bruno, 5));

        QualidadeDaCoorteDTO qualidade = useCase.execute();

        assertThat(qualidade.participantes()).isEqualTo(2);
        assertThat(qualidade.participantesComUmaResolucao()).isEqualTo(1);
    }

    /**
     * Os tres estados de consentimento particionam quem submeteu, e silencio nao e recusa: sao
     * numeros acionaveis diferentes — a um se convida de novo, ao outro nao se insiste.
     */
    @Test
    void separaQuemConsentiuDeQuemRecusouEDeQuemNaoRespondeu() {
        soJavaEhSuportado();
        cenario(Set.of(ana), Set.of(bruno), Set.of(ana, bruno, carla),
                CoorteDeTeste.analisada(ana, 4));

        QualidadeDaCoorteDTO qualidade = useCase.execute();

        assertThat(qualidade.participantesQueConsentiram()).isEqualTo(1);
        assertThat(qualidade.participantesQueRecusaram()).isEqualTo(1);
        assertThat(qualidade.participantesSemResposta()).isEqualTo(1);
        assertThat(qualidade.versaoDoTermo()).isEqualTo(VERSAO);
    }

    /**
     * O pesquisador precisa saber o tamanho do que nao esta vendo. Sem este numero, uma coorte de
     * 1 resolucao numa plataforma de 40 pareceria uma plataforma de 1 resolucao.
     */
    @Test
    void mostraQuantasResolucoesFicaramForaDaCoorte() {
        soJavaEhSuportado();
        when(termoDeConsentimentoPort.vigente())
                .thenReturn(new TermoDeConsentimento(VERSAO, "Termo", "texto", true));
        when(obterParticipantesConsentidosUseCase.historicoVigente())
                .thenReturn(historicoCom(Set.of(ana), Set.of(bruno)));
        when(coorteRepository.listarCoorte(Set.of(ana)))
                .thenReturn(List.of(CoorteDeTeste.analisada(ana, 4)));
        when(coorteRepository.autoresComResolucao()).thenReturn(Set.of(ana, bruno));
        when(coorteRepository.totalDeResolucoes()).thenReturn(9L);

        assertThat(useCase.execute().resolucoesForaDaCoorte()).isEqualTo(8);
    }

    /** A escala 1..5 aparece inteira: nivel sem nenhuma resolucao e informacao, nao ausencia. */
    @Test
    void aDistribuicaoDeAutonomiaCobreAEscalaInteira() {
        soJavaEhSuportado();
        coorte(CoorteDeTeste.analisada(ana, 4), CoorteDeTeste.analisada(bruno, 4));

        List<ContagemDTO> autonomia = useCase.execute().porAutonomia();

        assertThat(autonomia).hasSize(5).extracting(ContagemDTO::chave)
                .containsExactly("1", "2", "3", "4", "5");
        assertThat(autonomia).filteredOn(c -> c.chave().equals("4"))
                .singleElement().extracting(ContagemDTO::total).isEqualTo(2);
    }

    @Test
    void aLinguagemInformaSeExisteAnalisadorParaEla() {
        soJavaEhSuportado();
        coorte(CoorteDeTeste.analisada(ana, 4), CoorteDeTeste.semAnalisador(bruno, LinguagemProgramacao.PYTHON));

        assertThat(useCase.execute().porLinguagem())
                .anySatisfy(contagem -> {
                    assertThat(contagem.linguagem()).isEqualTo(LinguagemProgramacao.JAVA);
                    assertThat(contagem.comAnalisador()).isTrue();
                })
                .anySatisfy(contagem -> {
                    assertThat(contagem.linguagem()).isEqualTo(LinguagemProgramacao.PYTHON);
                    assertThat(contagem.comAnalisador()).isFalse();
                });
    }

    @Test
    void bancoVazioNaoQuebraEDevolveDatasNulas() {
        coorte();

        QualidadeDaCoorteDTO qualidade = useCase.execute();

        assertThat(qualidade.participantes()).isZero();
        assertThat(qualidade.resolucoes()).isZero();
        assertThat(qualidade.primeiraSubmissao()).isNull();
        assertThat(qualidade.ultimaSubmissao()).isNull();
        assertThat(qualidade.porAutonomia()).hasSize(5);
    }
}
