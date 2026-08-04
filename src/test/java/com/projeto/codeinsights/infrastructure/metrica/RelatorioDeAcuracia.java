package com.projeto.codeinsights.infrastructure.metrica;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.projeto.codeinsights.domain.knowledge.enums.ClasseComplexidade;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.CasoDeCorpus.Categoria;

/**
 * Transforma as medicoes do motor sobre o corpus num relatorio de acuracia em Markdown.
 * <p>
 * Aqui, ao contrario de {@link MotorDeMetricasCorpusTest}, a comparacao e contra o
 * <b>gabarito</b>: o numero produzido responde "quanto o motor acerta", nao "o motor mudou".
 */
final class RelatorioDeAcuracia {

    /** O que o motor respondeu para um caso, ao lado do gabarito que o caso carrega. */
    record Medida(
            CasoDeCorpus caso,
            String tempoObtido,
            NivelConfianca confiancaTempo,
            String espacoObtido,
            NivelConfianca confiancaEspaco) {
    }

    /**
     * Como a resposta do motor se posiciona em relacao ao gabarito. A direcao importa mais que o
     * acerto binario: <b>superestimar</b> avisa o aluno de um custo que ele nao tem (irritante,
     * mas seguro); <b>subestimar</b> diz que a solucao e melhor do que e, que e o erro perigoso
     * numa ferramenta de ensino.
     */
    private enum Direcao {
        ACERTO("acerto"),
        SUPERESTIMOU("superestimou"),
        SUBESTIMOU("subestimou"),
        RECUSOU("recusou classificar"),
        FORA_DA_ESCALA("fora da escala");

        private final String rotulo;

        Direcao(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    private RelatorioDeAcuracia() {
    }

    // ---------------------------------------------------------------- classificacao

    private static Direcao direcaoDoTempo(Medida medida) {
        return direcao(medida.caso().tempoGabarito(), medida.tempoObtido());
    }

    private static Direcao direcaoDoEspaco(Medida medida) {
        return direcao(medida.caso().espacoGabarito(), medida.espacoObtido());
    }

    private static Direcao direcao(String gabarito, String obtido) {
        if (!CasoDeCorpus.estaNaEscala(gabarito)) {
            return Direcao.FORA_DA_ESCALA;
        }
        if (gabarito.equals(obtido)) {
            return Direcao.ACERTO;
        }
        if (ClasseComplexidade.DESCONHECIDO.getRotulo().equals(obtido)) {
            return Direcao.RECUSOU;
        }
        return ordemDe(obtido) > ordemDe(gabarito) ? Direcao.SUPERESTIMOU : Direcao.SUBESTIMOU;
    }

    private static int ordemDe(String rotulo) {
        return classeDe(rotulo).map(ClasseComplexidade::getOrdem).orElse(-1);
    }

    private static Optional<ClasseComplexidade> classeDe(String rotulo) {
        return Arrays.stream(ClasseComplexidade.values())
                .filter(classe -> classe.getRotulo().equals(rotulo))
                .findFirst();
    }

    /** Um caso so entra no denominador da acuracia se o gabarito for representavel pelo motor. */
    private static boolean mensuravelNoTempo(Medida medida) {
        return CasoDeCorpus.estaNaEscala(medida.caso().tempoGabarito());
    }

    private static boolean mensuravelNoEspaco(Medida medida) {
        return medida.caso().espacoGabarito() != null
                && CasoDeCorpus.estaNaEscala(medida.caso().espacoGabarito());
    }

    // ---------------------------------------------------------------- resumo

    /**
     * Uma linha por linguagem, e nunca um numero so somando as duas: Java e C sao motores
     * diferentes, medidos por corpora diferentes, e um total agregado esconderia qual dos dois
     * esta errando — alem de se mover sozinho quando um dos corpora crescesse.
     */
    static String resumoDeUmaLinha(List<Medida> medidas) {
        List<String> porLinguagem = new ArrayList<>();
        for (LinguagemProgramacao linguagem : linguagensPresentes(medidas)) {
            List<Medida> daLinguagem = daLinguagem(medidas, linguagem);
            porLinguagem.add("%s %d casos: tempo %s, espaco %s".formatted(
                    linguagem, daLinguagem.size(),
                    percentual(daLinguagem, RelatorioDeAcuracia::mensuravelNoTempo,
                            RelatorioDeAcuracia::direcaoDoTempo),
                    percentual(daLinguagem, RelatorioDeAcuracia::mensuravelNoEspaco,
                            RelatorioDeAcuracia::direcaoDoEspaco)));
        }
        return "Acuracia do motor - " + String.join(" | ", porLinguagem) + ".";
    }

    private static List<LinguagemProgramacao> linguagensPresentes(List<Medida> medidas) {
        return Arrays.stream(LinguagemProgramacao.values())
                .filter(linguagem -> medidas.stream().anyMatch(m -> m.caso().linguagem() == linguagem))
                .toList();
    }

    private static List<Medida> daLinguagem(List<Medida> medidas, LinguagemProgramacao linguagem) {
        return medidas.stream().filter(m -> m.caso().linguagem() == linguagem).toList();
    }

    private static String percentual(List<Medida> medidas, Predicate<Medida> mensuravel,
            Function<Medida, Direcao> direcao) {
        long total = medidas.stream().filter(mensuravel).count();
        if (total == 0) {
            return "sem casos mensuraveis";
        }
        long acertos = medidas.stream().filter(mensuravel).filter(m -> direcao.apply(m) == Direcao.ACERTO).count();
        return "%d/%d (%s)".formatted(acertos, total, taxa(acertos, total));
    }

    private static String taxa(long acertos, long total) {
        return total == 0 ? "-" : String.format(Locale.ROOT, "%.1f%%", 100.0 * acertos / total);
    }

    // ---------------------------------------------------------------- markdown

    static String renderizar(List<Medida> medidas, LocalDate data) {
        StringBuilder md = new StringBuilder();
        cabecalho(md, medidas, data);
        resumoGeral(md, medidas);
        porCategoria(md, medidas);
        direcaoDoErro(md, medidas);
        acuraciaPorConfianca(md, medidas);
        divergencias(md, medidas);
        foraDaEscala(md, medidas);
        return md.toString();
    }

    private static void cabecalho(StringBuilder md, List<Medida> medidas, LocalDate data) {
        md.append("# Acuracia do motor de metricas\n\n");
        md.append("> **Arquivo gerado automaticamente — nao edite a mao.**\n");
        md.append("> Regenere com `./mvnw test -Dtest=RelatorioDeAcuraciaTest`.\n");
        md.append("> Ultima geracao: ").append(data).append(". Corpus: ")
                .append(medidas.size()).append(" casos.\n\n");

        md.append("## Como ler estes numeros\n\n");
        md.append("O corpus e o instrumento de validacao do motor: cada caso traz a complexidade correta ");
        md.append("segundo a literatura (o **gabarito**) e o motor e medido contra ela. ");
        md.append("Tres ressalvas mudam a leitura:\n\n");
        md.append("1. **Vies de autoria.** O corpus foi escrito por quem escreveu o motor. ");
        md.append("Um corpus assim conhece os pontos cegos do instrumento e tende a produzir acuracia ");
        md.append("otimista. O numero so vira evidencia forte quando houver solucoes reais de terceiros ");
        md.append("(Beecrowd, LeetCode, Codeforces) com Big O publicamente conhecido.\n");
        md.append("2. **A categoria importa mais que o total.** Acerto alto em `CANONICO` diz pouco — ");
        md.append("sao os algoritmos para os quais o motor foi desenhado. O numero que vale para o piloto ");
        md.append("e o de `ALUNO`.\n");
        md.append("3. **Espaco = memoria alocada pelo programa**, incluindo o vetor que guarda a entrada. ");
        md.append("Nao e o \"espaco auxiliar\" da literatura, que desconta a entrada.\n\n");
        md.append("A **complexidade ciclomatica nao aparece aqui**: McCabe e uma contagem exata de pontos ");
        md.append("de decisao, nao uma estimativa, e e validada caso a caso em `CiclomaticaAnalisadorTest`. ");
        md.append("Acuracia so faz sentido para o que o motor *estima*.\n\n");
    }

    private static void resumoGeral(StringBuilder md, List<Medida> medidas) {
        md.append("## Resumo por linguagem\n\n");
        md.append("Cada linguagem tem o seu motor e o seu corpus. Nao ha total somado de proposito: ");
        md.append("uma taxa unica misturaria dois analisadores e mudaria sozinha se um dos corpora ");
        md.append("crescesse mais que o outro.\n\n");
        md.append("| Linguagem | Casos | Big O de tempo | Complexidade de espaco |\n");
        md.append("|---|---:|---:|---:|\n");
        for (LinguagemProgramacao linguagem : linguagensPresentes(medidas)) {
            List<Medida> daLinguagem = daLinguagem(medidas, linguagem);
            md.append("| **%s** | %d | %s | %s |\n".formatted(
                    linguagem, daLinguagem.size(),
                    percentual(daLinguagem, RelatorioDeAcuracia::mensuravelNoTempo,
                            RelatorioDeAcuracia::direcaoDoTempo),
                    percentual(daLinguagem, RelatorioDeAcuracia::mensuravelNoEspaco,
                            RelatorioDeAcuracia::direcaoDoEspaco)));
        }
        md.append('\n');
    }

    private static void porCategoria(StringBuilder md, List<Medida> medidas) {
        md.append("## Acuracia por categoria\n\n");
        md.append("| Linguagem | Categoria | Casos | Tempo | Espaco |\n");
        md.append("|---|---|---:|---:|---:|\n");

        for (LinguagemProgramacao linguagem : linguagensPresentes(medidas)) {
            for (Categoria categoria : Categoria.values()) {
                List<Medida> daCategoria = daLinguagem(medidas, linguagem).stream()
                        .filter(m -> m.caso().categoria() == categoria)
                        .toList();
                if (daCategoria.isEmpty()) {
                    continue;
                }
                md.append("| %s | `%s` | %d | %s | %s |\n".formatted(
                        linguagem,
                        categoria,
                        daCategoria.size(),
                        percentual(daCategoria, RelatorioDeAcuracia::mensuravelNoTempo,
                                RelatorioDeAcuracia::direcaoDoTempo),
                        percentual(daCategoria, RelatorioDeAcuracia::mensuravelNoEspaco,
                                RelatorioDeAcuracia::direcaoDoEspaco)));
            }
        }
        md.append('\n');
    }

    private static void direcaoDoErro(StringBuilder md, List<Medida> medidas) {
        md.append("## Direcao do erro (Big O de tempo)\n\n");
        md.append("Subestimar e o erro perigoso: diz ao aluno que a solucao custa menos do que custa.\n\n");
        md.append("| Direcao |");
        linguagensPresentes(medidas).forEach(linguagem -> md.append(" %s |".formatted(linguagem)));
        md.append("\n|---|");
        linguagensPresentes(medidas).forEach(linguagem -> md.append("---:|"));
        md.append('\n');

        for (Direcao direcao : Direcao.values()) {
            md.append("| %s |".formatted(direcao.rotulo));
            for (LinguagemProgramacao linguagem : linguagensPresentes(medidas)) {
                md.append(" %d |".formatted(daLinguagem(medidas, linguagem).stream()
                        .filter(m -> direcaoDoTempo(m) == direcao).count()));
            }
            md.append('\n');
        }
        md.append('\n');
    }

    /**
     * A pergunta que esta tabela responde: a confianca declarada pelo motor <b>informa</b> alguma
     * coisa? Se a taxa de acerto em ALTA nao for maior que em MEDIA, o campo nao serve para filtrar
     * amostra na analise da pesquisa — e isso precisa ser sabido antes de usar o dado.
     */
    private static void acuraciaPorConfianca(StringBuilder md, List<Medida> medidas) {
        md.append("## Acerto por confianca declarada (Big O de tempo)\n\n");
        md.append("Se a taxa cai de `ALTA` para `BAIXA`, a confianca declarada pelo motor e **informativa** ");
        md.append("e pode ser usada para filtrar a amostra na analise da pesquisa. Se a coluna for plana, ");
        md.append("o campo nao carrega informacao e nao deve pesar em nenhum corte.\n\n");
        md.append("Em C a confianca **nunca chega a ALTA**: a estrutura do codigo e reconhecida por ");
        md.append("forma, sem parse completo da linguagem, e o teto fica em `MEDIA`. A linha `ALTA` ");
        md.append("vazia para C nao e falta de dado — e o teto funcionando.\n\n");
        md.append("| Linguagem | Confianca | Casos mensuraveis | Acertos | Taxa |\n");
        md.append("|---|---|---:|---:|---:|\n");

        for (LinguagemProgramacao linguagem : linguagensPresentes(medidas)) {
            for (NivelConfianca nivel : NivelConfianca.values()) {
                List<Medida> doNivel = daLinguagem(medidas, linguagem).stream()
                        .filter(RelatorioDeAcuracia::mensuravelNoTempo)
                        .filter(m -> m.confiancaTempo() == nivel)
                        .toList();
                long acertos = doNivel.stream().filter(m -> direcaoDoTempo(m) == Direcao.ACERTO).count();
                md.append("| %s | %s | %d | %d | %s |\n".formatted(linguagem, nivel, doNivel.size(),
                        acertos, taxa(acertos, doNivel.size())));
            }
        }
        md.append('\n');
    }

    private static void divergencias(StringBuilder md, List<Medida> medidas) {
        List<Medida> divergentes = medidas.stream()
                .filter(m -> (mensuravelNoTempo(m) && direcaoDoTempo(m) != Direcao.ACERTO)
                        || (mensuravelNoEspaco(m) && direcaoDoEspaco(m) != Direcao.ACERTO))
                .toList();

        md.append("## Divergencias\n\n");
        if (divergentes.isEmpty()) {
            md.append("Nenhuma: o motor bate com o gabarito em todos os casos mensuraveis.\n\n");
            return;
        }

        md.append("| Caso | Linguagem | Categoria | Metrica | Gabarito | Motor | Direcao | Confianca |\n");
        md.append("|---|---|---|---|---|---|---|---|\n");
        for (Medida medida : divergentes) {
            if (mensuravelNoTempo(medida) && direcaoDoTempo(medida) != Direcao.ACERTO) {
                linhaDeDivergencia(md, medida, "tempo", medida.caso().tempoGabarito(),
                        medida.tempoObtido(), direcaoDoTempo(medida), medida.confiancaTempo());
            }
            if (mensuravelNoEspaco(medida) && direcaoDoEspaco(medida) != Direcao.ACERTO) {
                linhaDeDivergencia(md, medida, "espaco", medida.caso().espacoGabarito(),
                        medida.espacoObtido(), direcaoDoEspaco(medida), medida.confiancaEspaco());
            }
        }
        md.append('\n');
        notas(md, divergentes);
    }

    private static void linhaDeDivergencia(StringBuilder md, Medida medida, String metrica,
            String gabarito, String obtido, Direcao direcao, NivelConfianca confianca) {
        md.append("| %s | %s | %s | %s | `%s` | `%s` | %s | %s |\n".formatted(
                medida.caso().nome(), medida.caso().linguagem(), medida.caso().categoria(), metrica,
                gabarito, obtido, direcao.rotulo, confianca));
    }

    private static void foraDaEscala(StringBuilder md, List<Medida> medidas) {
        List<Medida> fora = medidas.stream()
                .filter(m -> m.caso().tempoForaDaEscala() || m.caso().espacoForaDaEscala())
                .toList();

        md.append("## Gabaritos fora da escala\n\n");
        if (fora.isEmpty()) {
            md.append("Nenhum: todos os gabaritos cabem nas 8 classes do motor.\n\n");
            return;
        }

        md.append("Casos cuja complexidade correta o motor **nao consegue expressar** — nao e erro de ");
        md.append("estimativa, e limite da escala de 8 classes. Ficam fora do denominador da acuracia.\n\n");
        md.append("| Caso | Linguagem | Categoria | Gabarito | Resposta do motor |\n");
        md.append("|---|---|---|---|---|\n");
        for (Medida medida : fora) {
            md.append("| %s | %s | %s | `%s` | `%s` |\n".formatted(
                    medida.caso().nome(), medida.caso().linguagem(), medida.caso().categoria(),
                    medida.caso().tempoGabarito(), medida.tempoObtido()));
        }
        md.append('\n');
        notas(md, fora);
    }

    private static void notas(StringBuilder md, List<Medida> medidas) {
        List<Medida> comNota = medidas.stream().filter(m -> m.caso().nota() != null).toList();
        if (comNota.isEmpty()) {
            return;
        }
        List<String> linhas = new ArrayList<>();
        comNota.forEach(m -> linhas.add("- **%s** — %s".formatted(m.caso().nome(), m.caso().nota())));
        md.append(String.join("\n", linhas)).append("\n\n");
    }
}
