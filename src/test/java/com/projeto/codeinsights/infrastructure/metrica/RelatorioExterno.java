package com.projeto.codeinsights.infrastructure.metrica;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.projeto.codeinsights.infrastructure.metrica.ManifestoExterno.Contagens;
import com.projeto.codeinsights.infrastructure.metrica.ReducaoAoEscala.Metrica;

/**
 * O texto da secao {@code Categoria EXTERNO} do relatorio de acuracia (Adendos 6 e 7 do criterio do
 * corpus externo), gerado so dos numeros: nenhuma conclusao e escrita a mao, e a unica frase sobre H3
 * que muda com o resultado e o rotulo que {@link VereditoH3} devolve. O que e fixo sao os limites da
 * medicao, que valem para qualquer resultado.
 * <p>
 * Corpus externo nao entra em {@link CorpusDeAlgoritmos#casos()} nem em
 * {@link RelatorioDeAcuracia#renderizar}: esta secao tem renderizacao propria e as tabelas do
 * corpus proprio nao se mexem. O texto nunca traz codigo de terceiros, so identificadores da
 * anotacao e a resposta do motor.
 */
final class RelatorioExterno {

    static final String MOTOR = "5204882";
    static final String GABARITO = "a70adca";

    private RelatorioExterno() {
    }

    /** O corpo da secao, sem os marcadores (quem coloca e {@link BlocosDoRelatorio}). */
    static String renderizar(DadosDoBloco dados) {
        StringBuilder md = new StringBuilder();
        cabecalho(md, dados);
        declaracoes(md, dados);
        taxaDeTempo(md, dados);
        veredito(md, dados);
        espaco(md, dados);
        reducoes(md, dados);
        divergencias(md, dados);
        foraDaEscala(md, dados);
        direcaoDoErro(md, dados);
        confianca(md, dados);
        return md.toString();
    }

    private static void cabecalho(StringBuilder md, DadosDoBloco dados) {
        md.append("## Categoria EXTERNO\n\n");
        md.append("Solucoes reais de terceiros medidas pelo motor, lado a lado com o corpus proprio. ");
        md.append("Medicao de ").append(dados.data()).append(", gerada dos numeros; nao edite a mao. ");
        md.append("As regras estao nos Adendos 6 e 7 do criterio do corpus externo.\n\n");
    }

    // ---------------------------------------------------------------- declaracoes fixas

    private static void declaracoes(StringBuilder md, DadosDoBloco dados) {
        md.append("### O que esta medicao e, e o que ela nao e\n\n");
        md.append("- **Motor:** `main@").append(MOTOR).append("`, o da remedicao da IC-13; `src/main` e `pom.xml` ");
        md.append("nao mudam nesta medicao.\n");
        md.append("- **Gabarito:** `gabarito-externo.json` no commit `").append(GABARITO).append("`, que nao muda. ");
        md.append("A nota e o gabarito reduzido a escala so nos casos da tabela de reducoes (Adendo 6, itens 1 e 2).\n");
        md.append("- **Amostra:** a cota e de 20 casos por linguagem; neste bloco: ")
                .append(tamanhos(dados)).append(".\n");
        md.append("- **Uma plataforma so:** o corpus e so de LeetCode (Adendo 3, item 3), o que o enviesa para ");
        md.append("problemas de entrevista. Plataformas dos casos medidos: ").append(plataformas(dados)).append(".\n");
        md.append("- **Gabarito nao humano:** foi anotado por modelos de linguagem (Claude), em duas leituras ");
        md.append("independentes por linguagem, ambas da mesma familia de modelo (erros correlacionados); o motor ");
        md.append("foi escrito com ajuda do mesmo modelo (Adendo 4, item 1).\n");
        md.append("- **Recusa:** cada caso tem ").append(MedidorExterno.LIMITE_PADRAO.toSeconds());
        md.append(" s de analise; excecao, estouro de pilha ou de tempo contam como recusa e o caso fica ");
        md.append("(Adendo 7, item 1). `?` entra no denominador e nunca e acerto.\n");
        md.append("- **Funil de coleta (contagens do manifesto):**\n");
        for (ResumoExterno resumo : dados.resumos()) {
            md.append("  - ").append(resumo.linguagem()).append(": ")
                    .append(funil(dados.manifesto().contagens(resumo.linguagem()))).append('\n');
        }
        md.append("- **Como ler as taxas:** a *principal* so conta os casos cuja nota cabe nas 8 classes do motor; ");
        md.append("a *sensibilidade* conta os que estao fora da escala como erro; a *cobertura* e a fracao da ");
        md.append("amostra que a escala consegue expressar. Mesma conta nos dois corpora.\n\n");
    }

    private static String tamanhos(DadosDoBloco dados) {
        return dados.resumos().stream().map(r -> r.linguagem() + " " + r.medidas().size())
                .collect(Collectors.joining(", "));
    }

    private static String plataformas(DadosDoBloco dados) {
        Map<String, Long> porPlataforma = dados.externas().stream().collect(
                Collectors.groupingBy(m -> m.caso().plataforma(), TreeMap::new, Collectors.counting()));
        return porPlataforma.entrySet().stream().map(e -> "%s (%d)".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
    }

    /** Em ordem de motivo, para o texto nao mudar de uma execucao para outra. */
    private static String funil(Contagens contagens) {
        String exclusoes = new TreeMap<>(contagens.exclusoesPorMotivo()).entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(", "));
        return "%d percorridos, %d aceitos; exclusoes: %s".formatted(contagens.percorridos(), contagens.aceitos(),
                exclusoes.isEmpty() ? "nenhuma" : exclusoes);
    }

    // ---------------------------------------------------------------- numeros

    private static void taxaDeTempo(StringBuilder md, DadosDoBloco dados) {
        md.append("### Tempo: taxa principal, sensibilidade e cobertura\n\n");
        md.append(TabelasDeDesempenho.taxas(dados, Metrica.TEMPO));
    }

    private static void veredito(StringBuilder md, DadosDoBloco dados) {
        md.append("### Veredito de H3 (tempo, por linguagem)\n\n");
        md.append("H3: o motor acerta menos no corpus externo do que no proprio. Teste exato de Fisher bilateral, ");
        md.append("alfa de 0.05, so com a taxa principal de tempo; o lado do proprio e a base congelada ");
        md.append("(").append(BaseDoProprio.TEMPO_JAVA.acertos()).append('/')
                .append(BaseDoProprio.TEMPO_JAVA.denominador()).append(" em Java e ")
                .append(BaseDoProprio.TEMPO_C.acertos()).append('/').append(BaseDoProprio.TEMPO_C.denominador())
                .append(" em C), nao o que esta execucao mediu.\n\n");
        md.append(TabelasDeDesempenho.veredito(dados));
        md.append("**Limites que valem para qualquer resultado acima:**\n\n");
        md.append("- Sao **dois testes sem correcao para comparacoes multiplas** (um por linguagem).\n");
        md.append("- **Baixo poder:** a amostra so detecta quedas grandes (com N de 15 a 20, ate cerca de 80% em ");
        md.append("Java e 70% em C). Com os denominadores do Adendo 6, a queda so e significativa a partir de ");
        md.append("13/16 em Java e de 12/18 em C (3 e 6 erros).\n");
        md.append("- Queda sem significancia nunca e lida como refutacao: a amostra so enxerga quedas grandes. ");
        md.append("A mesma ressalva de amostra vale para taxa externa igual ou maior.\n");
        md.append("- Espaco, direcao do erro e confianca sao **descritivos, sem teste**.\n\n");
    }

    private static void espaco(StringBuilder md, DadosDoBloco dados) {
        md.append("### Espaco (descritivo, sem teste)\n\n");
        md.append(TabelasDeDesempenho.taxas(dados, Metrica.ESPACO));
    }

    private static void reducoes(StringBuilder md, DadosDoBloco dados) {
        md.append("### Reducoes a escala aplicadas na nota (Adendo 6, item 2)\n\n");
        md.append(TabelasDeCasos.reducoes(dados));
    }

    private static void divergencias(StringBuilder md, DadosDoBloco dados) {
        md.append("### Divergencias entre o motor e a nota\n\n");
        md.append("Recusas aparecem aqui com o motivo em codigo curto. Tempo esperado conta como os demais e ");
        md.append("vai marcado.\n\n");
        md.append(TabelasDeCasos.divergencias(dados));
        md.append(TabelasDeCasos.tempoEsperado(dados));
    }

    private static void foraDaEscala(StringBuilder md, DadosDoBloco dados) {
        md.append("### Gabaritos fora da escala (Adendo 6, item 3)\n\n");
        md.append("Limite da escala de 8 classes, nao erro de estimativa: ficam fora do denominador principal e ");
        md.append("entram como erro so na sensibilidade.\n\n");
        md.append(TabelasDeCasos.foraDaEscala(dados));
    }

    private static void direcaoDoErro(StringBuilder md, DadosDoBloco dados) {
        md.append("### Direcao do erro e recusas (tempo, descritivo)\n\n");
        md.append(TabelasDeDesempenho.direcaoDoErro(dados));
        md.append("Recusas do tempo entre os casos do denominador principal do externo:\n\n");
        md.append(TabelasDeDesempenho.recusas(dados));
    }

    private static void confianca(StringBuilder md, DadosDoBloco dados) {
        md.append("### Acerto por confianca declarada (tempo, descritivo)\n\n");
        md.append(TabelasDeDesempenho.confianca(dados));
    }
}
