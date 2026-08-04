package com.projeto.codeinsights.infrastructure.metrica;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.CasoDeCorpus.Categoria;

/**
 * Carrega o corpus de validacao do motor: os arquivos de codigo em
 * {@code src/test/resources/corpus/} mais o gabarito anotado a mao.
 * <p>
 * O codigo mora em arquivo de verdade, e nao em text block, por dois motivos praticos: colar
 * uma solucao de terceiro vira copiar-colar puro (sem escapar aspas nem barras), e o arquivo
 * pode ser aberto e lido como codigo. O gabarito fica separado porque e <b>anotacao humana</b>
 * sobre o codigo, nao parte dele.
 * <p>
 * <b>Um manifesto por linguagem.</b> Os casos de C nao sao traducao dos de Java: boa parte do
 * corpus Java usa {@code ArrayList}, {@code HashMap} e {@code String}, que nao tem equivalente em
 * C, e traduzir mediria o tradutor, nao o motor. Cada linguagem valida o motor com o codigo que
 * nela se escreve de verdade — e o relatorio reporta as duas separadamente, porque somar acertos
 * de motores diferentes num numero so esconderia qual dos dois esta errando.
 *
 * @see CasoDeCorpus para a distincao entre gabarito e esperado-do-motor
 */
final class CorpusDeAlgoritmos {

    /** Manifesto de uma linguagem e a pasta onde ficam os arquivos que ele lista. */
    private record Fonte(LinguagemProgramacao linguagem, String manifesto, String raiz) {
    }

    private static final String RAIZ_DO_CORPUS = "/corpus/";

    private static final List<Fonte> FONTES = List.of(
            new Fonte(LinguagemProgramacao.JAVA, "/corpus/gabarito.json", "/corpus/"),
            new Fonte(LinguagemProgramacao.C, "/corpus/gabarito-c.json", "/corpus/c/"));

    private static final ObjectMapper JSON = new ObjectMapper();

    private CorpusDeAlgoritmos() {
    }

    /** Entrada crua do manifesto. Campos ausentes chegam como {@code null}. */
    private record Entrada(
            String arquivo,
            String nome,
            String origem,
            String nota,
            String tempoGabarito,
            String tempoMotor,
            String espacoGabarito,
            String espacoMotor) {
    }

    private record Manifesto(List<Entrada> casos) {
    }

    static List<CasoDeCorpus> casos() {
        return FONTES.stream().flatMap(fonte -> casosDe(fonte).stream()).toList();
    }

    static List<CasoDeCorpus> casosDe(LinguagemProgramacao linguagem) {
        return casos().stream().filter(caso -> caso.linguagem() == linguagem).toList();
    }

    private static List<CasoDeCorpus> casosDe(Fonte fonte) {
        return lerManifesto(fonte.manifesto()).casos().stream()
                .map(entrada -> montar(entrada, fonte))
                .toList();
    }

    private static Manifesto lerManifesto(String caminho) {
        try (InputStream entrada = abrir(caminho)) {
            return JSON.readValue(entrada, Manifesto.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o manifesto " + caminho + ".", e);
        }
    }

    private static CasoDeCorpus montar(Entrada entrada, Fonte fonte) {
        String tempoEsperado = entrada.tempoMotor() != null ? entrada.tempoMotor() : entrada.tempoGabarito();
        String espacoEsperado = entrada.espacoMotor() != null ? entrada.espacoMotor() : entrada.espacoGabarito();

        exigirEsperadoExplicito(entrada, entrada.tempoGabarito(), entrada.tempoMotor(), "tempo");
        exigirEsperadoExplicito(entrada, entrada.espacoGabarito(), entrada.espacoMotor(), "espaco");

        return new CasoDeCorpus(
                entrada.nome(),
                fonte.linguagem(),
                categoriaDe(entrada.arquivo()),
                caminhoRelativoAoCorpus(entrada.arquivo(), fonte),
                lerCodigo(fonte.raiz() + entrada.arquivo()),
                entrada.origem(),
                entrada.nota(),
                entrada.tempoGabarito(),
                tempoEsperado,
                entrada.espacoGabarito(),
                espacoEsperado);
    }

    /**
     * Um gabarito fora da escala do motor (ex.: O(raiz de n)) nunca pode ser o valor esperado —
     * o motor e incapaz de emitir esse rotulo. Sem esta guarda o caso entraria com um esperado
     * impossivel e o teste falharia com uma mensagem que nao explica nada.
     */
    private static void exigirEsperadoExplicito(Entrada entrada, String gabarito, String motor, String metrica) {
        if (gabarito != null && !CasoDeCorpus.estaNaEscala(gabarito) && motor == null) {
            throw new IllegalStateException(
                    "%s: o gabarito de %s (%s) esta fora da escala do motor, entao o manifesto precisa dizer o que o motor responde hoje."
                            .formatted(entrada.arquivo(), metrica, gabarito));
        }
    }

    private static Categoria categoriaDe(String arquivo) {
        return Arrays.stream(arquivo.split("/"))
                .map(segmento -> segmento.toUpperCase(java.util.Locale.ROOT))
                .filter(CorpusDeAlgoritmos::ehCategoria)
                .map(Categoria::valueOf)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O caminho '%s' nao esta em nenhuma pasta de categoria.".formatted(arquivo)));
    }

    private static boolean ehCategoria(String segmento) {
        return Arrays.stream(Categoria.values()).anyMatch(categoria -> categoria.name().equals(segmento));
    }

    /** Caminho a partir de {@code /corpus/}, que e como o teste de sincronia com o disco compara. */
    private static String caminhoRelativoAoCorpus(String arquivo, Fonte fonte) {
        return fonte.raiz().substring(RAIZ_DO_CORPUS.length()) + arquivo;
    }

    private static String lerCodigo(String caminho) {
        try (InputStream entrada = abrir(caminho)) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o caso " + caminho + ".", e);
        }
    }

    private static InputStream abrir(String caminho) {
        InputStream entrada = CorpusDeAlgoritmos.class.getResourceAsStream(caminho);
        if (entrada == null) {
            throw new IllegalStateException("Recurso do corpus nao encontrado: " + caminho);
        }
        return entrada;
    }
}
