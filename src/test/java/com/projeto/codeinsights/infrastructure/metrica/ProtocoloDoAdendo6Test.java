package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.ReducaoAoEscala.Metrica;
import com.projeto.codeinsights.infrastructure.metrica.ReducaoAoEscala.Reducao;

/**
 * Confere o gabarito e o manifesto reais contra o que o Adendo 6 do criterio escreveu antes de
 * medir: os denominadores, a lista de fora da escala, a tabela de reducao, os casos de tempo esperado
 * e a ligacao entre gabarito e manifesto. Le so anotacao commitada; nao roda o motor e nao abre fonte
 * nenhuma. Se um numero aqui nao bate, o que mudou foi o gabarito ou o protocolo, e a medicao nao
 * deve seguir ate alguem decidir qual dos dois esta certo.
 */
class ProtocoloDoAdendo6Test {

    private static GabaritoExterno gabarito;
    private static ManifestoExterno manifesto;

    @BeforeAll
    static void lerAnotacoes() {
        gabarito = GabaritoExterno.dosResources();
        manifesto = ManifestoExterno.dosResources();
    }

    @Test
    void sao20CasosPorLinguagem() {
        assertThat(gabarito.casos(LinguagemProgramacao.JAVA)).hasSize(20);
        assertThat(gabarito.casos(LinguagemProgramacao.C)).hasSize(20);
    }

    @Test
    void denominadoresDeTempoSao16EmJavaE18EmC() {
        assertThat(contar(LinguagemProgramacao.JAVA, CasoExterno::tempoNaEscala)).isEqualTo(16);
        assertThat(contar(LinguagemProgramacao.C, CasoExterno::tempoNaEscala)).isEqualTo(18);
    }

    @Test
    void denominadoresDeEspacoSao18EmJavaE19EmC() {
        assertThat(contar(LinguagemProgramacao.JAVA, CasoExterno::espacoNaEscala)).isEqualTo(18);
        assertThat(contar(LinguagemProgramacao.C, CasoExterno::espacoNaEscala)).isEqualTo(19);
    }

    @Test
    void foraDaEscalaNoTempoEExatamenteAListaDoAdendo() {
        assertThat(ids(LinguagemProgramacao.JAVA, caso -> !caso.tempoNaEscala()))
                .containsExactlyInAnyOrder("037-1482", "039-3195", "045-885", "085-2305");
        assertThat(ids(LinguagemProgramacao.C, caso -> !caso.tempoNaEscala()))
                .containsExactlyInAnyOrder("247-2125", "535-2326");
    }

    @Test
    void foraDaEscalaNoEspacoEExatamenteAListaDoAdendo() {
        assertThat(ids(LinguagemProgramacao.JAVA, caso -> !caso.espacoNaEscala()))
                .containsExactlyInAnyOrder("045-885", "085-2305");
        assertThat(ids(LinguagemProgramacao.C, caso -> !caso.espacoNaEscala()))
                .containsExactlyInAnyOrder("535-2326");
    }

    @Test
    void asReducoesAplicadasSaoExatamenteAsCincoDaTabela() {
        List<String> aplicadas = todosOsCasos().stream()
                .flatMap(caso -> reducoesDe(caso).stream())
                .toList();

        assertThat(aplicadas).containsExactlyInAnyOrder(
                "021-3022 TEMPO", "049-1334 TEMPO", "315-2225 TEMPO", "315-2225 ESPACO", "373-392 TEMPO");
        assertThat(aplicadas).hasSameSizeAs(ReducaoAoEscala.tabela());
    }

    @Test
    void todaReducaoDaTabelaTemOCasoNoGabaritoComOTextoRegistrado() {
        Set<String> arquivos = todosOsCasos().stream().map(CasoExterno::arquivo).collect(Collectors.toSet());

        assertThat(ReducaoAoEscala.tabela()).extracting(Reducao::arquivo).allMatch(arquivos::contains);
    }

    @Test
    void osQuatroCasosDeTempoEsperadoSaoDeJava() {
        assertThat(ids(LinguagemProgramacao.JAVA, CasoExterno::tempoEsperado))
                .containsExactlyInAnyOrder("009-2097", "013-2588", "031-3679", "135-4066");
        assertThat(ids(LinguagemProgramacao.C, CasoExterno::tempoEsperado)).isEmpty();
    }

    @Test
    void osArquivosDoGabaritoSaoOsDoManifesto() {
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            assertThat(manifesto.sha256PorArquivo(linguagem).keySet())
                    .as("arquivos do manifesto em %s", linguagem)
                    .isEqualTo(gabarito.arquivos(linguagem));
        }
    }

    @Test
    void todoSha256DoManifestoTem64DigitosHexadecimais() {
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            assertThat(manifesto.sha256PorArquivo(linguagem).values())
                    .as("sha256 em %s", linguagem)
                    .hasSize(20)
                    .allMatch(hash -> hash.matches("[0-9a-f]{64}"));
        }
    }

    @Test
    void contagensDoFunilFecham() {
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            ManifestoExterno.Contagens contagens = manifesto.contagens(linguagem);
            int excluidos = contagens.exclusoesPorMotivo().values().stream().mapToInt(Integer::intValue).sum();

            assertThat(contagens.aceitos()).as("aceitos em %s", linguagem).isEqualTo(20);
            assertThat(contagens.aceitos() + excluidos)
                    .as("aceitos + excluidos = percorridos em %s", linguagem)
                    .isEqualTo(contagens.percorridos());
        }
        assertThat(manifesto.contagens(LinguagemProgramacao.JAVA).percorridos()).isEqualTo(135);
        assertThat(manifesto.contagens(LinguagemProgramacao.C).percorridos()).isEqualTo(535);
    }

    private static List<CasoExterno> todosOsCasos() {
        return GabaritoExterno.LINGUAGENS.stream().flatMap(l -> gabarito.casos(l).stream()).toList();
    }

    private static long contar(LinguagemProgramacao linguagem, Predicate<CasoExterno> filtro) {
        return gabarito.casos(linguagem).stream().filter(filtro).count();
    }

    private static List<String> ids(LinguagemProgramacao linguagem, Predicate<CasoExterno> filtro) {
        return gabarito.casos(linguagem).stream().filter(filtro).map(CasoExterno::id).toList();
    }

    private static List<String> reducoesDe(CasoExterno caso) {
        List<String> reducoes = new ArrayList<>();
        if (caso.tempoReduzido()) {
            reducoes.add(caso.id() + " " + Metrica.TEMPO);
        }
        if (caso.espacoReduzido()) {
            reducoes.add(caso.id() + " " + Metrica.ESPACO);
        }
        return reducoes;
    }
}
