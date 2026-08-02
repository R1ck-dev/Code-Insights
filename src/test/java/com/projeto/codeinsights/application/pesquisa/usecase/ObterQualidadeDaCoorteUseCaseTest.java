package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;

@ExtendWith(MockitoExtension.class)
class ObterQualidadeDaCoorteUseCaseTest {

    @Mock
    private CoorteRepository coorteRepository;
    @Mock
    private AnalisadorMetricas analisadorMetricas;

    @InjectMocks
    private ObterQualidadeDaCoorteUseCase useCase;

    private final UUID ana = UUID.randomUUID();
    private final UUID bruno = UUID.randomUUID();

    private void soJavaEhSuportado() {
        when(analisadorMetricas.suporta(any())).thenAnswer(chamada ->
                chamada.getArgument(0) == LinguagemProgramacao.JAVA);
    }

    private void coorte(ResolucaoDaCoorte... resolucoes) {
        when(coorteRepository.listarCoorte()).thenReturn(List.of(resolucoes));
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
