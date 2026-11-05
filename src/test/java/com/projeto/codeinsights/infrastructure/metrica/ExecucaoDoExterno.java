package com.projeto.codeinsights.infrastructure.metrica;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import com.projeto.codeinsights.infrastructure.metrica.MedidorDoCorpus.Medicao;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * A medicao do corpus externo de ponta a ponta, com tudo por parametro: a pasta dos fontes, o
 * gabarito, o manifesto, o relatorio e as medidas do corpus proprio (ou um fornecedor delas). A
 * classe nao sabe onde fica nenhuma pasta nem le o motor por conta propria, o que deixa o pipeline
 * inteiro testavel com fixtures sinteticas.
 * <p>
 * A ordem protege a primeira execucao (Adendo 6, item 9, e Adendo 7, itens 2 e 3):
 * <ol>
 *   <li>confere que o relatorio existe, que o bloco externo pode ser trocado e que ele ainda e o aviso
 *       de "nao medido" (a medicao e unica: sobre um bloco medido, so com {@link Modo#REMEDIR});</li>
 *   <li>mede o corpus proprio e confere a base congelada (54/55 e 37/40 no tempo): se der outra, para;</li>
 *   <li>so entao carrega os fontes (SHA-256 contra o manifesto) e mede;</li>
 *   <li>monta o texto inteiro e, por ultimo, troca o bloco externo do relatorio.</li>
 * </ol>
 * Qualquer falha antes do ultimo passo deixa o arquivo como estava, e a recusa do primeiro passo nao
 * mede o proprio nem toca nos fontes.
 */
final class ExecucaoDoExterno {

    /** As medidas do externo e o corpo da secao gerada (sem os marcadores). */
    record Resultado(List<MedidaExterna> medidas, String bloco) {
    }

    /** {@code MEDIR} so escreve sobre o aviso de nao medido; {@code REMEDIR} e o pedido explicito de sobrescrever. */
    enum Modo { MEDIR, REMEDIR }

    private ExecucaoDoExterno() {
    }

    /** Com o corpus proprio medido de verdade, o limite de tempo padrao e a data de hoje. */
    static Resultado executar(Path pasta, GabaritoExterno gabarito, ManifestoExterno manifesto, Path relatorio,
            Modo modo) {
        return executar(pasta, gabarito, manifesto, relatorio, ExecucaoDoExterno::medirOProprio,
                new MedidorExterno(), LocalDate.now(), modo);
    }

    static Resultado executar(Path pasta, GabaritoExterno gabarito, ManifestoExterno manifesto, Path relatorio) {
        return executar(pasta, gabarito, manifesto, relatorio, Modo.MEDIR);
    }

    static Resultado executar(Path pasta, GabaritoExterno gabarito, ManifestoExterno manifesto, Path relatorio,
            Supplier<List<Medida>> medidasDoProprio, MedidorExterno medidor, LocalDate data) {
        return executar(pasta, gabarito, manifesto, relatorio, medidasDoProprio, medidor, data, Modo.MEDIR);
    }

    static Resultado executar(Path pasta, GabaritoExterno gabarito, ManifestoExterno manifesto, Path relatorio,
            Supplier<List<Medida>> medidasDoProprio, MedidorExterno medidor, LocalDate data, Modo modo) {
        BlocosDoRelatorio.exigirSubstituivel(relatorio);
        exigirPermissao(relatorio, modo);

        List<Medida> proprias = medidasDoProprio.get();
        BaseDoProprio.exigir(proprias);

        List<MedidaExterna> medidas = CorpusExternoLocal.carregar(pasta, gabarito, manifesto).stream()
                .map(medidor::medir)
                .toList();
        String bloco = RelatorioExterno.renderizar(new DadosDoBloco(medidas, proprias, manifesto, data));

        BlocosDoRelatorio.substituirExterno(relatorio, bloco);
        return new Resultado(medidas, bloco);
    }

    /** Recusa, sem medir nem apagar nada, sobrescrever um bloco que nao e o aviso de nao medido. */
    private static void exigirPermissao(Path relatorio, Modo modo) {
        if (modo == Modo.MEDIR && !BlocosDoRelatorio.externoNaoMedido(relatorio)) {
            throw new IllegalStateException("O relatorio ja tem o bloco externo medido, e a medicao e unica "
                    + "(Adendo 6, item 9). Nada foi medido nem apagado, e os fontes nao foram tocados. Para refazer por defeito do teste ou "
                    + "do relatorio, peca a remedicao explicita (corpus.externo=remedir).");
        }
    }

    /** O corpus proprio inteiro, pelo mesmo caminho que o {@code RelatorioDeAcuraciaTest} usa. */
    static List<Medida> medirOProprio() {
        return CorpusDeAlgoritmos.casos().stream().map(ExecucaoDoExterno::medir).toList();
    }

    private static Medida medir(CasoDeCorpus caso) {
        Medicao medicao = MedidorDoCorpus.medir(caso);
        return new Medida(caso, medicao.tempo(), medicao.confiancaTempo(), medicao.espaco(),
                medicao.confiancaEspaco());
    }
}
