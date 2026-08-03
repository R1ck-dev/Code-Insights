package com.projeto.codeinsights.infrastructure.persistence.pesquisa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.infrastructure.persistence.pesquisa.repository.SpringDataCoorteRepository;

/**
 * A projecao da coorte e <b>posicional</b>: a JPQL devolve {@code Object[]} e o adapter atribui por
 * indice. Trocar duas colunas de lugar compila, roda, e produz dado errado em silencio — espaco no
 * campo de tempo, confianca de uma metrica atribuida a outra.
 * <p>
 * Cada posicao aqui recebe um valor <b>distinguivel das demais</b> exatamente para que uma
 * transposicao nao possa passar: se a ordem do select mudar sem a do record, este teste cai.
 */
@ExtendWith(MockitoExtension.class)
class CoorteRepositoryAdapterTest {

    @Mock
    private SpringDataCoorteRepository springDataCoorteRepository;

    @InjectMocks
    private CoorteRepositoryAdapter adapter;

    private static final UUID RESOLUCAO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AUTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DESAFIO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime SUBMETIDA = OffsetDateTime.parse("2026-08-01T10:00:00Z");
    /** Distinto de SUBMETIDA de proposito: iguais esconderiam a troca entre as duas datas. */
    private static final OffsetDateTime ANALISADA = OffsetDateTime.parse("2026-08-02T15:30:00Z");

    /** A testemunha de tipo evita que o varargs de {@code List.of} espalhe o array em varios itens. */
    private void retorna(Object[] linha) {
        when(springDataCoorteRepository.coorte(any(), any(), any(), any()))
                .thenReturn(List.<Object[]>of(linha));
    }

    /**
     * Conjunto vazio nao pode virar {@code in ()} — nao e SQL valido. O adapter responde sem tocar no
     * banco, e "ninguem consentiu" e uma resposta completa, nao um caso degenerado.
     */
    @Test
    void semParticipantesConsentidosNaoConsultaOBanco() {
        assertThat(adapter.listarCoorte(Set.of())).isEmpty();

        verifyNoInteractions(springDataCoorteRepository);
    }

    @Test
    void mapeiaCadaColunaDoSelectParaOComponenteCerto() {
        retorna(new Object[] {
                RESOLUCAO, AUTOR, DESAFIO, "Two Sum",
                LinguagemProgramacao.PYTHON, 4, true,
                "O(n^2)", 4, NivelConfianca.ALTA,
                "O(log n)", 1, NivelConfianca.BAIXA,
                7, SUBMETIDA, ANALISADA });

        ResolucaoDaCoorte resolucao = adapter.listarCoorte(Set.of(AUTOR)).get(0);

        assertThat(resolucao.resolucaoId()).isEqualTo(RESOLUCAO);
        assertThat(resolucao.autorId()).isEqualTo(AUTOR);
        assertThat(resolucao.desafioId()).isEqualTo(DESAFIO);
        assertThat(resolucao.desafioTitulo()).isEqualTo("Two Sum");
        assertThat(resolucao.linguagem()).isEqualTo(LinguagemProgramacao.PYTHON);
        assertThat(resolucao.indiceAutonomiaIA()).isEqualTo(4);
        assertThat(resolucao.analisada()).isTrue();
        // Tempo e espaco tem valores distintos de proposito: iguais esconderiam a troca entre eles.
        assertThat(resolucao.tempoRotulo()).isEqualTo("O(n^2)");
        assertThat(resolucao.tempoOrdem()).isEqualTo(4);
        assertThat(resolucao.confiancaTempo()).isEqualTo(NivelConfianca.ALTA);
        assertThat(resolucao.espacoRotulo()).isEqualTo("O(log n)");
        assertThat(resolucao.espacoOrdem()).isEqualTo(1);
        assertThat(resolucao.confiancaEspaco()).isEqualTo(NivelConfianca.BAIXA);
        assertThat(resolucao.ciclomatica()).isEqualTo(7);
        assertThat(resolucao.submetidaEm()).isEqualTo(SUBMETIDA);
        assertThat(resolucao.analisadoEm()).isEqualTo(ANALISADA);
    }

    /**
     * Resolucao nao analisada casa com nenhum dos tres left join e vem com nulos. O nulo precisa
     * sobreviver: {@code 0} seria O(1), uma complexidade legitima, e a analise nao conseguiria
     * separar "otima" de "sem dado".
     */
    @Test
    void preservaOsNulosDeQuemNaoCasouComNenhumaMetrica() {
        retorna(new Object[] {
                RESOLUCAO, AUTOR, DESAFIO, "Two Sum",
                LinguagemProgramacao.JAVA, 3, false,
                null, null, null,
                null, null, null,
                null, SUBMETIDA, null });

        ResolucaoDaCoorte resolucao = adapter.listarCoorte(Set.of(AUTOR)).get(0);

        assertThat(resolucao.tempoOrdem()).isNull();
        assertThat(resolucao.espacoOrdem()).isNull();
        assertThat(resolucao.ciclomatica()).isNull();
        assertThat(resolucao.temMetrica()).isFalse();
        // Sem metrica gravada nao ha instante de analise. E a invariante que o CSV publica: celula
        // vazia em analisado_em significa "o motor nao produziu numero", nunca "produziu e esqueci".
        assertThat(resolucao.analisadoEm()).isNull();
    }

    /**
     * O Hibernate devolve inteiro de projecao como {@code Long} em varios caminhos; um cast direto
     * para {@code Integer} lancaria ClassCastException so em producao, com dado real.
     */
    @Test
    void aceitaInteiroVindoComoLongDaProjecao() {
        retorna(new Object[] {
                RESOLUCAO, AUTOR, DESAFIO, "Two Sum",
                LinguagemProgramacao.JAVA, 5L, true,
                "O(n)", 2L, NivelConfianca.ALTA,
                "O(1)", 0L, NivelConfianca.ALTA,
                3L, SUBMETIDA, ANALISADA });

        ResolucaoDaCoorte resolucao = adapter.listarCoorte(Set.of(AUTOR)).get(0);

        assertThat(resolucao.indiceAutonomiaIA()).isEqualTo(5);
        assertThat(resolucao.tempoOrdem()).isEqualTo(2);
        assertThat(resolucao.espacoOrdem()).isZero();
        assertThat(resolucao.ciclomatica()).isEqualTo(3);
    }
}
