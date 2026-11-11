package com.projeto.codeinsights.infrastructure.metrica;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.ReducaoAoEscala.Metrica;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Direcao;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * As tabelas de numeros do bloco externo (taxas, veredito de H3, direcao do erro, recusas e
 * confianca), proprio e externo lado a lado, por linguagem e nunca somadas. Cada metodo devolve uma
 * tabela inteira em Markdown; so o veredito de H3 testa hipotese.
 */
final class TabelasDeDesempenho {

    private TabelasDeDesempenho() {
    }

    // ---------------------------------------------------------------- taxas

    /** Taxa principal com Wilson de 95%, sensibilidade e cobertura da escala, por linguagem e corpus. */
    static String taxas(DadosDoBloco dados, Metrica metrica) {
        TabelaMd tabela = new TabelaMd("Linguagem", "Corpus", "Taxa principal", "IC 95% (Wilson)",
                "Sensibilidade (fora da escala = erro)", "Cobertura da escala");
        for (ResumoExterno resumo : dados.resumos()) {
            List<Medida> proprias = dados.propriasDe(resumo.linguagem());
            Desempenho proprio = metrica == Metrica.TEMPO
                    ? Desempenho.doProprioTempo(proprias) : Desempenho.doProprioEspaco(proprias);
            Desempenho externo = metrica == Metrica.TEMPO ? resumo.tempo() : resumo.espaco();
            tabela.linha(resumo.linguagem().toString(), "proprio", proprio.principal().texto(),
                    proprio.principal().intervalo(), proprio.sensibilidade().texto(), proprio.cobertura());
            tabela.linha(resumo.linguagem().toString(), "EXTERNO", externo.principal().texto(),
                    externo.principal().intervalo(), externo.sensibilidade().texto(), externo.cobertura());
        }
        return tabela.renderizar();
    }

    // ---------------------------------------------------------------- veredito de H3

    /**
     * Fisher bilateral entre a base congelada do proprio e a taxa principal de tempo do externo. O
     * rotulo do veredito e o que {@link VereditoH3} devolve; nada alem disso e concluido aqui.
     */
    static String veredito(DadosDoBloco dados) {
        TabelaMd tabela = new TabelaMd("Linguagem", "Proprio (base)", "Externo (principal)", "Taxa externa",
                "p (Fisher bilateral)", "Veredito");
        for (ResumoExterno resumo : dados.resumos()) {
            TaxaDeAcerto base = BaseDoProprio.tempo(resumo.linguagem());
            TaxaDeAcerto externo = resumo.tempo().principal();
            if (externo.denominador() == 0) {
                tabela.linha(resumo.linguagem().toString(), base.texto(), externo.texto(), "-", "-",
                        "sem veredito: nenhum caso no denominador externo");
                continue;
            }
            tabela.linha(resumo.linguagem().toString(), base.texto(), externo.texto(),
                    comparacao(base, externo), valorP(base, externo), rotulo(vereditoDe(base, externo)));
        }
        return tabela.renderizar();
    }

    private static VereditoH3 vereditoDe(TaxaDeAcerto base, TaxaDeAcerto externo) {
        return VereditoH3.de(base.acertos(), base.denominador(), externo.acertos(), externo.denominador());
    }

    private static String comparacao(TaxaDeAcerto base, TaxaDeAcerto externo) {
        int ordem = VereditoH3.compararExternaComProprio(base.acertos(), base.denominador(),
                externo.acertos(), externo.denominador());
        return ordem < 0 ? "menor" : ordem == 0 ? "igual" : "maior";
    }

    private static String valorP(TaxaDeAcerto base, TaxaDeAcerto externo) {
        double p = EstatisticaExata.fisherBilateral(base.acertos(), base.denominador(),
                externo.acertos(), externo.denominador());
        return String.format(Locale.ROOT, "%.4f", p);
    }

    private static String rotulo(VereditoH3 veredito) {
        return switch (veredito) {
            case SUSTENTADA -> "H3 sustentada";
            case INCONCLUSIVO -> "inconclusivo";
            case NAO_SUSTENTADA -> "H3 nao sustentada";
        };
    }

    // ---------------------------------------------------------------- direcao do erro e recusas

    /** Contagem por direcao no tempo, em todos os casos de cada corpus (fora da escala incluido). */
    static String direcaoDoErro(DadosDoBloco dados) {
        List<ResumoExterno> resumos = dados.resumos();
        TabelaMd tabela = new TabelaMd(colunasPorLinguagem(resumos));
        for (Direcao direcao : Direcao.values()) {
            String[] linha = new String[1 + 2 * resumos.size()];
            linha[0] = direcao.rotulo();
            for (int i = 0; i < resumos.size(); i++) {
                ResumoExterno resumo = resumos.get(i);
                linha[1 + 2 * i] = String.valueOf(dados.propriasDe(resumo.linguagem()).stream()
                        .filter(m -> RelatorioDeAcuracia.direcaoDoTempo(m) == direcao).count());
                linha[2 + 2 * i] = String.valueOf(resumo.contarNoTempo(direcao));
            }
            tabela.linha(linha);
        }
        return tabela.renderizar();
    }

    /** Por que o motor nao respondeu o tempo dos casos do denominador principal do externo. */
    static String recusas(DadosDoBloco dados) {
        List<ResumoExterno> resumos = dados.resumos();
        TabelaMd tabela = new TabelaMd(colunasExternas(resumos));
        for (MotivoRecusa motivo : MotivoRecusa.values()) {
            tabela.linha(linhaExterna(motivo.name(), resumos, resumo -> resumo.recusasNoTempo(motivo)));
        }
        tabela.linha(linhaExterna("motor respondeu ? (sem falha)", resumos, ResumoExterno::indeterminadosNoTempo));
        return tabela.renderizar();
    }

    private static String[] colunasPorLinguagem(List<ResumoExterno> resumos) {
        String[] colunas = new String[1 + 2 * resumos.size()];
        colunas[0] = "Direcao";
        for (int i = 0; i < resumos.size(); i++) {
            colunas[1 + 2 * i] = resumos.get(i).linguagem() + " proprio";
            colunas[2 + 2 * i] = resumos.get(i).linguagem() + " EXTERNO";
        }
        return colunas;
    }

    private static String[] colunasExternas(List<ResumoExterno> resumos) {
        String[] colunas = new String[1 + resumos.size()];
        colunas[0] = "Motivo";
        for (int i = 0; i < resumos.size(); i++) {
            colunas[1 + i] = resumos.get(i).linguagem() + " EXTERNO";
        }
        return colunas;
    }

    private static String[] linhaExterna(String rotulo, List<ResumoExterno> resumos,
            Function<ResumoExterno, Long> valor) {
        String[] linha = new String[1 + resumos.size()];
        linha[0] = rotulo;
        for (int i = 0; i < resumos.size(); i++) {
            linha[1 + i] = String.valueOf(valor.apply(resumos.get(i)));
        }
        return linha;
    }

    // ---------------------------------------------------------------- confianca

    /** Acerto de tempo por confianca declarada, nos dois corpora; descritivo, sem teste. */
    static String confianca(DadosDoBloco dados) {
        TabelaMd tabela = new TabelaMd("Linguagem", "Corpus", "Confianca", "Taxa principal");
        for (ResumoExterno resumo : dados.resumos()) {
            List<Medida> proprias = dados.propriasDe(resumo.linguagem());
            for (NivelConfianca nivel : NivelConfianca.values()) {
                Desempenho proprio = Desempenho.doProprioTempo(
                        proprias.stream().filter(m -> m.confiancaTempo() == nivel).toList());
                tabela.linha(resumo.linguagem().toString(), "proprio", nivel.name(), proprio.principal().texto());
                tabela.linha(resumo.linguagem().toString(), "EXTERNO", nivel.name(),
                        resumo.tempoComConfianca(nivel).principal().texto());
            }
        }
        return tabela.renderizar();
    }
}
