package com.projeto.codeinsights.infrastructure.metrica;

import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.FORA_DA_ESCALA;
import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.caso;
import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.recusada;
import static com.projeto.codeinsights.infrastructure.metrica.MedidasSinteticas.respondida;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Direcao;

/**
 * A classificacao do corpus externo contra a nota, com a mesma regra de direcao do corpus proprio e
 * as regras do Adendo 6, itens 3 a 5: `?` e recusa ficam no denominador sem ser acerto, e o que esta
 * fora da escala sai do denominador principal e entra como erro so na sensibilidade.
 */
class ResumoExternoTest {

    private static final LinguagemProgramacao JAVA = LinguagemProgramacao.JAVA;
    private static final LinguagemProgramacao C = LinguagemProgramacao.C;

    private static ResumoExterno resumoDe(List<MedidaExterna> medidas, LinguagemProgramacao linguagem) {
        return ResumoExterno.porLinguagem(medidas).stream().filter(r -> r.linguagem() == linguagem).findFirst()
                .orElseThrow();
    }

    @Test
    void acertoEClasseIdenticaAoGabarito() {
        MedidaExterna medida = respondida(caso(JAVA, "001"), "O(n)", "O(1)");

        assertThat(ResumoExterno.direcaoDoTempo(medida)).isEqualTo(Direcao.ACERTO);
        assertThat(ResumoExterno.direcaoDoEspaco(medida)).isEqualTo(Direcao.ACERTO);
    }

    @Test
    void superestimouEMotorMaisCaroQueAGabarito() {
        MedidaExterna medida = respondida(caso(JAVA, "001"), "O(n^2)", "O(n)");

        assertThat(ResumoExterno.direcaoDoTempo(medida)).isEqualTo(Direcao.SUPERESTIMOU);
        assertThat(ResumoExterno.direcaoDoEspaco(medida)).isEqualTo(Direcao.SUPERESTIMOU);
    }

    @Test
    void subestimouEMotorMaisBarato() {
        CasoExterno quadratico = caso(JAVA, "001", "O(n^2)", "O(n^2)", "O(n)", "O(n)", "pior-caso");

        assertThat(ResumoExterno.direcaoDoTempo(respondida(quadratico, "O(n)", "O(1)"))).isEqualTo(Direcao.SUBESTIMOU);
        assertThat(ResumoExterno.direcaoDoEspaco(respondida(quadratico, "O(n)", "O(1)")))
                .isEqualTo(Direcao.SUBESTIMOU);
    }

    @Test
    void recusaNuncaEAcertoEFicaNoDenominador() {
        List<MedidaExterna> medidas = List.of(
                respondida(caso(JAVA, "001"), "O(n)", "O(1)"),
                recusada(caso(JAVA, "002"), MotivoRecusa.FALHA, "ParseProblemException"),
                recusada(caso(JAVA, "003"), MotivoRecusa.TEMPO_ESGOTADO, null));
        ResumoExterno resumo = resumoDe(medidas, JAVA);

        assertThat(ResumoExterno.direcaoDoTempo(medidas.get(1))).isEqualTo(Direcao.RECUSOU);
        assertThat(resumo.tempo().principal().texto()).startsWith("1/3");
        assertThat(resumo.espaco().principal().texto()).startsWith("1/3");
        assertThat(resumo.recusasNoTempo(MotivoRecusa.FALHA)).isEqualTo(1);
        assertThat(resumo.recusasNoTempo(MotivoRecusa.TEMPO_ESGOTADO)).isEqualTo(1);
        assertThat(resumo.recusasNoTempo(MotivoRecusa.SEM_METRICA)).isZero();
    }

    @Test
    void interrogacaoDoProprioMotorEntraNoDenominadorSemSerRecusaComMotivo() {
        MedidaExterna indeterminada = respondida(caso(C, "001"), "?", "O(1)");
        ResumoExterno resumo = resumoDe(List.of(indeterminada), C);

        assertThat(ResumoExterno.direcaoDoTempo(indeterminada)).isEqualTo(Direcao.RECUSOU);
        assertThat(resumo.tempo().principal().texto()).startsWith("0/1");
        assertThat(resumo.indeterminadosNoTempo()).isEqualTo(1);
        assertThat(resumo.recusasNoTempo(MotivoRecusa.SEM_METRICA)).isZero();
    }

    @Test
    void foraDaEscalaSaiDoDenominadorPrincipalEEntraComoErroNaSensibilidade() {
        CasoExterno multivariavel = caso(JAVA, "003", FORA_DA_ESCALA, FORA_DA_ESCALA, "O(1)", "O(1)", "pior-caso");
        List<MedidaExterna> medidas = List.of(
                respondida(caso(JAVA, "001"), "O(n)", "O(1)"),
                respondida(caso(JAVA, "002"), "O(n^2)", "O(1)"),
                respondida(multivariavel, "O(n)", "O(1)"));
        ResumoExterno resumo = resumoDe(medidas, JAVA);

        assertThat(ResumoExterno.direcaoDoTempo(medidas.get(2))).isEqualTo(Direcao.FORA_DA_ESCALA);
        assertThat(resumo.tempo()).isEqualTo(new Desempenho(1, 2, 3));
        assertThat(resumo.tempo().principal().texto()).isEqualTo("1/2 (50.0%)");
        assertThat(resumo.tempo().sensibilidade().texto()).isEqualTo("1/3 (33.3%)");
        assertThat(resumo.tempo().cobertura()).isEqualTo("2/3 (66.7%)");
        assertThat(resumo.foraDaEscalaNoTempo()).containsExactly(medidas.get(2));
        assertThat(resumo.divergentesNoTempo()).containsExactly(medidas.get(1));
    }

    @Test
    void reducaoAplicadaComparaOMotorComANotaEMostraAsDuasFormas() {
        CasoExterno reduzido = caso(C, "315", "O(m)", "O(n)", "O(m)", "O(n)", "pior-caso");
        MedidaExterna medida = respondida(reduzido, "O(n)", "O(n)");

        assertThat(ResumoExterno.direcaoDoTempo(medida)).isEqualTo(Direcao.ACERTO);
        assertThat(ResumoExterno.direcaoDoEspaco(medida)).isEqualTo(Direcao.ACERTO);
        assertThat(reduzido.tempoReduzido()).isTrue();
        assertThat(reduzido.tempoGabarito()).isEqualTo("O(m)");
        assertThat(resumoDe(List.of(medida), C).tempo().principal().texto()).startsWith("1/1");
    }

    @Test
    void espacoSemGabaritoNaoEntraNemComoErro() {
        CasoExterno semEspaco = caso(JAVA, "001", "O(n)", "O(n)", null, null, "pior-caso");
        MedidaExterna medida = respondida(semEspaco, "O(n)", "O(1)");
        ResumoExterno resumo = resumoDe(List.of(medida, respondida(caso(JAVA, "002"), "O(n)", "O(1)")), JAVA);

        assertThat(ResumoExterno.espacoAnotado(medida)).isFalse();
        assertThat(resumo.espaco()).isEqualTo(new Desempenho(1, 1, 1));
        assertThat(resumo.espaco().cobertura()).isEqualTo("1/1 (100.0%)");
    }

    @Test
    void espacoForaDaEscalaFicaFora() {
        CasoExterno espacoMultivariavel = caso(C, "535", "O(n)", "O(n)", FORA_DA_ESCALA, FORA_DA_ESCALA, "pior-caso");
        MedidaExterna medida = respondida(espacoMultivariavel, "O(n)", "O(n)");
        ResumoExterno resumo = resumoDe(List.of(medida), C);

        assertThat(resumo.foraDaEscalaNoEspaco()).containsExactly(medida);
        assertThat(resumo.espaco()).isEqualTo(new Desempenho(0, 0, 1));
        assertThat(resumo.espaco().principal().texto()).isEqualTo(TaxaDeAcerto.SEM_CASOS);
    }

    @Test
    void tempoEsperadoContaComoOsDemaisEFicaIdentificavel() {
        CasoExterno esperado = caso(JAVA, "001", "O(n)", "O(n)", "O(1)", "O(1)", "esperado");
        List<MedidaExterna> medidas = List.of(respondida(esperado, "O(n)", "O(1)"),
                respondida(caso(JAVA, "002"), "O(n)", "O(1)"));
        ResumoExterno resumo = resumoDe(medidas, JAVA);

        assertThat(resumo.tempo().principal().texto()).startsWith("2/2");
        assertThat(resumo.tempoEsperado()).isEqualTo(new Desempenho(1, 1, 1));
    }

    @Test
    void linguagensNaoSeSomam() {
        List<MedidaExterna> medidas = new ArrayList<>(MedidasSinteticas.lote(JAVA, 3, 4, 1));
        medidas.addAll(MedidasSinteticas.lote(C, 1, 2, 0));

        List<ResumoExterno> resumos = ResumoExterno.porLinguagem(medidas);

        assertThat(resumos).extracting(ResumoExterno::linguagem).containsExactly(JAVA, C);
        assertThat(resumos.get(0).tempo()).isEqualTo(new Desempenho(3, 4, 5));
        assertThat(resumos.get(1).tempo()).isEqualTo(new Desempenho(1, 2, 2));
    }

    @Test
    void linguagemSemCasosNaoGeraResumo() {
        assertThat(ResumoExterno.porLinguagem(MedidasSinteticas.lote(C, 1, 1, 0)))
                .extracting(ResumoExterno::linguagem).containsExactly(C);
    }

    @Test
    void confiancaDeclaradaSeparaOsAcertos() {
        MedidaExterna media = respondida(caso(JAVA, "001"), "O(n)", "O(1)");
        MedidaExterna recusa = recusada(caso(JAVA, "002"), MotivoRecusa.FALHA, "IllegalStateException");
        ResumoExterno resumo = resumoDe(List.of(media, recusa), JAVA);

        assertThat(resumo.tempoComConfianca(NivelConfianca.MEDIA).principal().texto()).startsWith("1/1");
        assertThat(resumo.tempoComConfianca(NivelConfianca.BAIXA).principal().texto()).startsWith("0/1");
        assertThat(resumo.tempoComConfianca(NivelConfianca.ALTA).principal().texto())
                .isEqualTo(TaxaDeAcerto.SEM_CASOS);
    }
}
